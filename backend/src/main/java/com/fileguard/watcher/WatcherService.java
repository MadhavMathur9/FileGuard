  package com.fileguard.watcher;
  

import jakarta.annotation.PostConstruct;

 import jakarta.annotation.PreDestroy;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;


import java.io.IOException; 

import java.nio.file.*;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;    

import java.util.concurrent.ConcurrentHashMap;  
import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
   import lombok.RequiredArgsConstructor;
   
import com.fileguard.pipeline.EventIngester;
   import com.fileguard.reconcile.ReconciliationService;

import static java.nio.file.StandardWatchEventKinds.*; 

@Service

    @Slf4j

    @RequiredArgsConstructor

public class WatcherService {

       private final EventIngester eventIngester;

    private final ReconciliationService reconciliationService;

    private WatchService watchService;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
      private final ExecutorService executor = Executors.newSingleThreadExecutor();    


        @PostConstruct   
    public void init() throws IOException {     

        this.watchService = FileSystems.getDefault().newWatchService();
        executor.submit(this::processEvents);
    }

    @PreDestroy
    public void cleanup() {
        executor.shutdownNow();
        try {     
            if (watchService != null) {
                watchService.close();
              }
            executor.awaitTermination(2, TimeUnit.SECONDS);   
          } catch (InterruptedException | IOException e) {
            log.error("Error shutting down WatcherService", e);

          }
     }

       public void registerAll(final Path start) throws IOException {     
        // register directory and sub-directories
         Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {     
                   registerDirectory(dir);
                return FileVisitResult.CONTINUE;

                }
        });     
    }

    private void registerDirectory(Path dir) throws IOException {
         WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
        log.info("Registered watcher for: {}", dir);
    }

       private void processEvents() {
        while (!Thread.currentThread().isInterrupted()) { 
            WatchKey key;
            try {   
                key = watchService.take();


            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();

                return;
            } catch (ClosedWatchServiceException e) {
                log.info("Watch service closed, stopping event processing.");
                return;
                }
   
            Path dir = keys.get(key);
            if (dir == null) {

                   log.warn("WatchKey not recognized!");
                   continue;
            }     

               for (WatchEvent<?> event : key.pollEvents()) {

                WatchEvent.Kind<?> kind = event.kind();
    
                if (kind == OVERFLOW) {
                    log.warn("Overflow event for directory: {}", dir);  
                        reconciliationService.reconcileDirectory(dir);
                    continue;
                }


                // Context for directory entry event is the file name of entry
                 @SuppressWarnings("unchecked")

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                 Path name = ev.context();

                Path child = dir.resolve(name);

                log.info("Raw event: {} on {}", kind.name(), child);


                // Dispatch to the ingester pipeline 
                eventIngester.ingest(child, kind);

                // if directory is created, and watching recursively, then register it and its sub-directories


                if (kind == ENTRY_CREATE) {  
                       try {
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            registerAll(child);
                         }
                    } catch (IOException x) {
                        log.error("Failed to register new directory: {}", child, x);
                    }
                   }
            }

                // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                 keys.remove(key);

                 log.info("WatchKey for {} is no longer valid. Removed.", dir);

                if (keys.isEmpty()) {

                    log.info("All directories are inaccessible.");
                }     

            }

        }   
     } 
}   
