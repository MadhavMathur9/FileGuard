   package com.fileguard.reconcile;

import com.fileguard.domain.FileEntry;
import com.fileguard.domain.WatchedFolder;
import com.fileguard.pipeline.EventIngester;

 import com.fileguard.repository.FileEntryRepository;
import com.fileguard.repository.WatchedFolderRepository;
   import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 import org.springframework.boot.context.event.ApplicationReadyEvent;
  import org.springframework.context.event.EventListener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;   

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import java.util.Map;

import java.util.Set;
import java.util.stream.Collectors;


import java.util.stream.Stream;

    import static java.nio.file.StandardWatchEventKinds.*;


@Service
@Slf4j  
  @RequiredArgsConstructor

public class ReconciliationService {


        private final WatchedFolderRepository folderRepository;   
    private final FileEntryRepository fileEntryRepository;

        private final EventIngester eventIngester;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.fileguard.watcher.WatcherService watcherService;
   
    @EventListener(ApplicationReadyEvent.class)  

    public void onStartup() {

        log.info("Starting startup reconciliation...");
        List<WatchedFolder> folders = folderRepository.findAll();
        for (WatchedFolder folder : folders) {

            reconcileFolder(folder.getId(), false);
            try {
                   watcherService.registerAll(Paths.get(folder.getPath()));
            } catch (IOException e) {
                   log.error("Failed to register watcher for {}", folder.getPath(), e);

            }    
        } 
        log.info("Startup reconciliation complete.");     
    }
    
    @Async
        public void reconcileFolderAsync(Long folderId, boolean deep) {     
        reconcileFolder(folderId, deep);
    }


    public void reconcileFolder(Long folderId, boolean deep) {
        WatchedFolder folder = folderRepository.findById(folderId).orElse(null);
        if (folder == null) return;

    
        Path rootPath = Paths.get(folder.getPath());
        if (!Files.exists(rootPath)) {

             log.warn("Folder {} no longer exists on disk, skipping reconciliation.", rootPath);
             return;

        }    

           log.info("Reconciling folder: {} (deep={})", rootPath, deep); 
        
        List<FileEntry> indexedFiles = fileEntryRepository.findByFolderId(folderId);

         Map<String, FileEntry> indexMap = indexedFiles.stream()

                .collect(Collectors.toMap(FileEntry::getPath, f -> f));

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile).forEach(path -> { 
                FileEntry entry = indexMap.remove(path.toString());
                
                try {
                       long size = Files.size(path);
                      long mtime = Files.getLastModifiedTime(path).toMillis();

                    if (entry == null) {
                        log.debug("Found new file during reconcile: {}", path);
                        eventIngester.ingest(path, ENTRY_CREATE);
                    } else {

                          long entryMtimeMillis = entry.getMtime() != null ? entry.getMtime().toInstant().toEpochMilli() : 0L;     

                            if (deep || entry.getSize() != size || entryMtimeMillis != mtime) {

                             log.debug("Found modified file during reconcile: {}", path);    
                            eventIngester.ingest(path, ENTRY_MODIFY); 
                         }
                    }

                 } catch (IOException e) {     
                        log.warn("Failed to stat file during reconcile: {}", path, e);

                }

               });
        } catch (IOException e) {
            log.error("Failed to walk folder during reconcile: {}", rootPath, e);
        }
  
         // Remaining files in indexMap are no longer on disk   
        for (FileEntry missing : indexMap.values()) {

              Path missingPath = rootPath.resolve(missing.getPath());   
            log.debug("Found deleted file during reconcile: {}", missingPath);
            eventIngester.ingest(missingPath, ENTRY_DELETE);    
        }
    }


    @Async

    public void reconcileDirectory(Path dir) {  
        // OVERFLOW handler: find which WatchedFolder this belongs to
            List<WatchedFolder> folders = folderRepository.findAll();


        for (WatchedFolder folder : folders) {
            Path root = Paths.get(folder.getPath());
            if (dir.startsWith(root)) {

                log.info("Triggering overflow reconciliation for folder: {}", folder.getPath());
                reconcileFolder(folder.getId(), false);    

                return;
            }
        }
    }
}

