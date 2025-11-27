package com.fileguard.pipeline;

import com.fileguard.domain.FileEntry; 
import com.fileguard.domain.WatchedFolder;

import com.fileguard.events.EventType;
  import com.fileguard.events.FileEvent;
import com.fileguard.hashing.HashService;

import com.fileguard.repository.FileEntryRepository; 
  import com.fileguard.repository.WatchedFolderRepository;

import jakarta.annotation.PostConstruct;  
import jakarta.annotation.PreDestroy;
import com.fileguard.integrity.IntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
   
import java.io.IOException;   
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;

import java.util.Optional;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import java.util.concurrent.ConcurrentHashMap;

    import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
    
@Service
@Slf4j
@RequiredArgsConstructor
public class HashWorkerPool {

    private final HashService hashService;
    private final FileEntryRepository fileEntryRepository;
       private final WatchedFolderRepository folderRepository;


    private final AnalyzerService analyzerService;

     private final IntegrityService integrityService;


        private BlockingQueue<FileEvent> queue;    
    private ExecutorService virtualExecutor;
     private final ConcurrentHashMap<Long, Object> sizeLocks = new ConcurrentHashMap<>();


     @Value("${fileguard.pipeline.queue-capacity:10000}")

    private int queueCapacity;     

    @Value("${fileguard.pipeline.workers:8}") 
    private int workers;
    
    @PostConstruct
    public void init() {
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
          this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
        for (int i = 0; i < workers; i++) {
            virtualExecutor.submit(this::workerLoop);    
        }
           log.info("Started HashWorkerPool with {} virtual threads, queue capacity: {}", workers, queueCapacity); 
    }


    @PreDestroy
    public void cleanup() {  

         virtualExecutor.shutdownNow();

        try {   
            virtualExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

            log.error("Interrupted during worker shutdown", e);
        }
       }

    public void submit(FileEvent event) {

        try {
            // Blocks if the queue is full (providing backpressure)
            queue.put(event);
        } catch (InterruptedException e) {     

            Thread.currentThread().interrupt();
            log.error("Interrupted while submitting event to queue: {}", event, e);     

        }
    }


    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {

                try {
                    FileEvent event = queue.take();
                  processEvent(event);

            } catch (InterruptedException e) {
                 Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                   log.error("Unhandled error in hashing worker", e);
            }

        }
    }  

    private void processEvent(FileEvent event) {
          log.info("Hashing worker processing: {}", event.getPath());

        if (event.getType() == EventType.DELETED) {
            handleDeletion(event.getPath());
            return;
        }
   
          try {
            WatchedFolder folder = findFolder(event.getPath()).orElse(null);
            if (folder == null) return;
            Long folderId = folder.getId();
   
            Optional<FileEntry> existingOpt = fileEntryRepository.findByFolderIdAndPath(folderId, event.getPath().toString()); 
            FileEntry entry = existingOpt.orElseGet(FileEntry::new);
            
            entry.setFolderId(folderId);


              entry.setPath(event.getPath().toString());  
            entry.setSize(event.getSize());
                entry.setMtime(OffsetDateTime.parse(event.getMtime().toString()));

            entry.setStatus("INDEXED");  

              entry.setLastSeenAt(OffsetDateTime.now());

            Object lock = sizeLocks.computeIfAbsent(event.getSize(), k -> new Object());

            synchronized (lock) {

                    // 1. Group by size
                List<FileEntry> sameSize = fileEntryRepository.findBySize(event.getSize());
            // Filter out self if updating
            sameSize.removeIf(e -> e.getId() != null && e.getId().equals(entry.getId()));


            if (sameSize.isEmpty()) {
                entry.setPartialHash(null);
                   entry.setFullHash(null);
            } else {
                // 2. Hash first 4KB
                 String thisPartial = hashService.computePartialHash(event.getPath());
                entry.setPartialHash(thisPartial);
                
                // Backfill partial hashes for colliding size entries

                for (FileEntry other : sameSize) {
                        if (other.getPartialHash() == null) {


                        try {

                            other.setPartialHash(hashService.computePartialHash(Paths.get(other.getPath())));
                            fileEntryRepository.save(other);
                        } catch (IOException e) {
                            log.error("Could not backfill partial hash for {}", other.getPath(), e);
                        }
                    }
                  }
                
                 // 3. Check for partial hash collision

                 List<FileEntry> samePartial = fileEntryRepository.findBySizeAndPartialHash(event.getSize(), thisPartial);   
                    samePartial.removeIf(e -> e.getId() != null && e.getId().equals(entry.getId()));
                
                if (samePartial.isEmpty()) {
                       entry.setFullHash(null);  
                    } else {
                    // 4. Compute full SHA-256     
                        String thisFull = hashService.computeFullHash(event.getPath());

                     entry.setFullHash(thisFull);
 
                     // Backfill full hashes

                    for (FileEntry other : samePartial) {
                        if (other.getFullHash() == null) {
                               try {

                                other.setFullHash(hashService.computeFullHash(Paths.get(other.getPath()))); 

                                fileEntryRepository.save(other);    
                                analyzerService.analyzeDuplicates(other.getFullHash(), other);

                                integrityService.checkFile(other, folder.getMode()); 
                            } catch (IOException e) {
                                log.error("Could not backfill full hash for {}", other.getPath(), e);
                            }
                           }
                    }
                }
            }


            fileEntryRepository.save(entry);
             } // end synchronized    
            if (entry.getFullHash() != null) {  
                analyzerService.analyzeDuplicates(entry.getFullHash(), entry);
             }

            integrityService.checkFile(entry, folder.getMode());

            log.info("Saved FileEntry for {}", event.getPath());
        } catch (IOException e) {

            log.error("Failed to hash file: {}", event.getPath(), e);

          }
       }   

    private void handleDeletion(Path path) {
        WatchedFolder folder = findFolder(path).orElse(null);
        if (folder == null) return;

         Long folderId = folder.getId();
        
        integrityService.checkDeleted(folderId, path.toString(), folder.getMode());
        
        fileEntryRepository.findByFolderIdAndPath(folderId, path.toString())
            .ifPresent(fileEntry -> {
                String fullHash = fileEntry.getFullHash();     
                fileEntryRepository.delete(fileEntry); 
                if (fullHash != null) {

                    analyzerService.analyzeDuplicates(fullHash, null);     
                }

            });
          log.info("Deleted FileEntry for {}", path);
    }

    private Optional<WatchedFolder> findFolder(Path filePath) {
        return folderRepository.findAll().stream() 
                   .filter(f -> filePath.startsWith(Paths.get(f.getPath())))
                 .findFirst();
    }

}   
