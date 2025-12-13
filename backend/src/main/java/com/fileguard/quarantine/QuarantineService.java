package com.fileguard.quarantine;

import com.fileguard.domain.QuarantineItem;
import com.fileguard.policy.FileOps;  

   import com.fileguard.repository.QuarantineItemRepository;
  import lombok.RequiredArgsConstructor;     
import lombok.extern.slf4j.Slf4j;  

 import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

 
import java.io.IOException;    
import java.nio.file.Files;
    import java.nio.file.Path;    

 import java.nio.file.Paths;     

 import java.time.Instant;
  import java.util.List; 

    @Service

@Slf4j
@RequiredArgsConstructor
 public class QuarantineService {

    private final QuarantineItemRepository quarantineItemRepository;
       private final FileOps fileOps;

    @Transactional

    public void restore(Long quarantineId) throws IOException {

        QuarantineItem item = quarantineItemRepository.findById(quarantineId)

                .orElseThrow(() -> new IllegalArgumentException("Quarantine item not found"));

        if (item.getRestoredAt() != null) {


            throw new IllegalStateException("Item is already restored");   
        }
  
            Path source = Paths.get(item.getQuarantinePath());
            Path target = Paths.get(item.getOriginalPath());
  
        // Basic conflict handling: if target exists, we could rename it or fail
        // For now, let's fail to avoid overwriting user data
        if (Files.exists(target)) {
            throw new IllegalStateException("Cannot restore, original path is occupied: " + target);
          }
    
        if (!Files.exists(source)) {
               throw new IllegalStateException("Quarantined file missing at " + source);  
        }


         fileOps.move(source, target);
  
        item.setRestoredAt(Instant.now());

        quarantineItemRepository.save(item);

        
        log.info("Restored quarantined file from {} to {}", source, target);
    }  

    @Transactional

        public void purge(Long quarantineId) throws IOException {
           QuarantineItem item = quarantineItemRepository.findById(quarantineId)

                .orElseThrow(() -> new IllegalArgumentException("Quarantine item not found"));
  
        if (item.getRestoredAt() != null) {
                throw new IllegalStateException("Cannot purge an item that was restored");

         }   

        Path source = Paths.get(item.getQuarantinePath());   

        fileOps.delete(source);
        
        quarantineItemRepository.delete(item);

          log.info("Purged quarantined file {}", source);   
    }
    
    // Cron job to automatically purge expired items
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    @Transactional

    public void purgeExpired() {
        List<QuarantineItem> expiredItems = quarantineItemRepository.findByExpiresAtBeforeAndRestoredAtIsNull(Instant.now());
        for (QuarantineItem item : expiredItems) {
            try {    

                purge(item.getId());
            } catch (Exception e) {
                log.error("Failed to automatically purge quarantine item {}", item.getId(), e); 
              }
         }   
    }
}
