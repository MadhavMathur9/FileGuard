package com.fileguard.integrity; 
    
import com.fileguard.domain.BaselineEntry;
import com.fileguard.domain.FileEntry;
import com.fileguard.domain.IntegrityAlert;
import com.fileguard.domain.WatchedFolder;
import com.fileguard.repository.BaselineEntryRepository;
import com.fileguard.repository.FileEntryRepository;
  import com.fileguard.repository.IntegrityAlertRepository;  
import com.fileguard.repository.WatchedFolderRepository;


import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;  

import java.time.Instant;

 import java.util.List;
import java.util.Optional;


   @Service  

@Slf4j
@RequiredArgsConstructor    

public class IntegrityService {   
   
    private final BaselineEntryRepository baselineEntryRepository;

    private final IntegrityAlertRepository integrityAlertRepository;
    private final WatchedFolderRepository watchedFolderRepository;
      private final FileEntryRepository fileEntryRepository;

    @Transactional
    public void createBaseline(Long folderId) {
        WatchedFolder folder = watchedFolderRepository.findById(folderId)
                  .orElseThrow(() -> new IllegalArgumentException("Folder not found"));


        if (!"INTEGRITY".equals(folder.getMode()) && !"BOTH".equals(folder.getMode())) {
            throw new IllegalStateException("Folder is not in INTEGRITY or BOTH mode");     
        }
   
        // Clear existing baseline

        List<BaselineEntry> existing = baselineEntryRepository.findByFolderId(folderId);
        baselineEntryRepository.deleteAll(existing);
  
          // Fetch all current files and create baseline entries


        List<FileEntry> files = fileEntryRepository.findByFolderId(folderId);   

        for (FileEntry file : files) {
            BaselineEntry baseline = new BaselineEntry();
            baseline.setFolder(folder);
            baseline.setPath(file.getPath());   
            baseline.setFullHash(file.getFullHash());
             baseline.setSize(file.getSize());
            baseline.setMtime(file.getMtime());

            baseline.setApprovedAt(Instant.now());
            baselineEntryRepository.save(baseline);
        }


          log.info("Created baseline for folder {} with {} files", folder.getPath(), files.size());     
    }    

    @Transactional
        public void checkFile(FileEntry entry, String folderMode) {
        if (!"INTEGRITY".equals(folderMode) && !"BOTH".equals(folderMode)) {  
            return;
        }


        Long folderId = entry.getFolderId();
         String path = entry.getPath();
    
        Optional<BaselineEntry> baselineOpt = baselineEntryRepository.findByFolderIdAndPath(folderId, path); 

        if (baselineOpt.isPresent()) {
            BaselineEntry baseline = baselineOpt.get();
             if (entry.getFullHash() != null && !entry.getFullHash().equals(baseline.getFullHash())) {
                  raiseAlert(folderId, "MODIFIED", path, baseline.getFullHash(), entry.getFullHash());    

            }
            } else {
            // Check if renamed (same hash, different path in baseline)
               List<BaselineEntry> baselinesWithHash = baselineEntryRepository.findByFolderIdAndFullHash(folderId, entry.getFullHash());  
            if (!baselinesWithHash.isEmpty()) {
                  // Heuristic: pick first as the renamed file
                BaselineEntry oldBaseline = baselinesWithHash.get(0);


                 raiseAlert(folderId, "RENAMED", path, oldBaseline.getFullHash(), entry.getFullHash()); 

            } else {
                raiseAlert(folderId, "ADDED", path, null, entry.getFullHash());
             }

        }

    }


    @Transactional

    public void checkDeleted(Long folderId, String path, String folderMode) {

        if (!"INTEGRITY".equals(folderMode) && !"BOTH".equals(folderMode)) {
            return;
        }
  
        Optional<BaselineEntry> baselineOpt = baselineEntryRepository.findByFolderIdAndPath(folderId, path);
        if (baselineOpt.isPresent()) {     
                raiseAlert(folderId, "REMOVED", path, baselineOpt.get().getFullHash(), null);    

        }
    }
 

    private void raiseAlert(Long folderId, String type, String path, String oldHash, String newHash) {
           // Debounce: check if OPEN alert exists for this path   
          Optional<IntegrityAlert> existing = integrityAlertRepository.findByFolderIdAndPathAndState(folderId, path, "OPEN");

        if (existing.isPresent()) {   
            IntegrityAlert alert = existing.get();
               // Update the alert if necessary
            alert.setNewHash(newHash);     

            alert.setType(type); // Might transition from ADDED to MODIFIED
            integrityAlertRepository.save(alert);  
            log.info("Updated integrity alert for {} ({})", path, type);
              return;   
        }

        IntegrityAlert alert = new IntegrityAlert();    
         WatchedFolder folder = watchedFolderRepository.findById(folderId).orElseThrow();
        alert.setFolder(folder);
        alert.setType(type);
        alert.setPath(path);
        alert.setOldHash(oldHash);
        alert.setNewHash(newHash);     
        alert.setState("OPEN");

        

        integrityAlertRepository.save(alert);
        log.info("Raised integrity alert for {} ({})", path, type);   
       }
   
    @Transactional
    public void acceptAlert(Long alertId) {
         IntegrityAlert alert = integrityAlertRepository.findById(alertId)

                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        
        if (!"OPEN".equals(alert.getState())) {

            throw new IllegalStateException("Only OPEN alerts can be accepted");

        }
        
            alert.setState("ACCEPTED_AS_NEW_BASELINE");
        alert.setResolvedAt(Instant.now());
        integrityAlertRepository.save(alert);
        

        // Update baseline

        if ("REMOVED".equals(alert.getType()) || "RENAMED".equals(alert.getType())) {
            // For renamed, the old path baseline should be removed 
            // Wait, renamed means we added a new baseline for new path and we remove the old path baseline
            baselineEntryRepository.findByFolderIdAndPath(alert.getFolder().getId(), alert.getPath())
                        .ifPresent(baselineEntryRepository::delete);    
        } 
    
        if ("MODIFIED".equals(alert.getType()) || "ADDED".equals(alert.getType()) || "RENAMED".equals(alert.getType())) {
             FileEntry entry = fileEntryRepository.findByFolderIdAndPath(alert.getFolder().getId(), alert.getPath())   
                    .orElse(null);
            
            if (entry != null) {


                  BaselineEntry baseline = baselineEntryRepository.findByFolderIdAndPath(alert.getFolder().getId(), alert.getPath())
                        .orElse(new BaselineEntry());
                  baseline.setFolder(alert.getFolder());

                  baseline.setPath(entry.getPath());
                baseline.setFullHash(entry.getFullHash());
                 baseline.setSize(entry.getSize());  

                baseline.setMtime(entry.getMtime());


                baseline.setApprovedAt(Instant.now());
                baselineEntryRepository.save(baseline);

            }
        }
     }
    
    @Transactional
    public void acknowledgeAlert(Long alertId) {
        IntegrityAlert alert = integrityAlertRepository.findById(alertId)
                   .orElseThrow(() -> new IllegalArgumentException("Alert not found"));     
   
        if (!"OPEN".equals(alert.getState())) {
            throw new IllegalStateException("Only OPEN alerts can be acknowledged");

         }    
 
         alert.setState("ACKNOWLEDGED");

        alert.setResolvedAt(Instant.now());

        integrityAlertRepository.save(alert);
    }
}
