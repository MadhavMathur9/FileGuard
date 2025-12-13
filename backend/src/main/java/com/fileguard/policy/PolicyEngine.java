package com.fileguard.policy;

 import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileguard.domain.*;  
import com.fileguard.repository.AuditLogRepository;
import com.fileguard.repository.FolderRuleRepository;
import com.fileguard.repository.QuarantineItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
   
import java.io.IOException;
   import java.nio.file.Path;   
 import java.nio.file.Paths;
import java.util.List;     
import java.util.Map;    
import java.util.UUID;

@Service
  @Slf4j
@RequiredArgsConstructor
public class PolicyEngine {

        private final FolderRuleRepository folderRuleRepository;
    private final FileOps fileOps;
    private final QuarantineItemRepository quarantineItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper; 



     // Configurable quarantine directory
    private final Path quarantineDir = Paths.get(System.getProperty("java.io.tmpdir"), "fileguard_quarantine");
  
    @Transactional
    public void evaluateDuplicate(FileEntry duplicateFile, FileEntry originalFile) {    
        if (duplicateFile.getId().equals(originalFile.getId())) {
            return;
        }

        List<FolderRule> rules = folderRuleRepository.findByFolderIdAndEnabledTrue(duplicateFile.getFolderId());
        
        FolderRule rule = rules.stream()
                .filter(r -> "DUPLICATE".equalsIgnoreCase(r.getTrigger()))
                .findFirst()
                .orElse(null);

           if (rule == null) {
            // Default action is NOTIFY

            logAction(null, "NOTIFY", duplicateFile, "Default policy applied. Duplicate found.", false);
             return;
        }

           applyAction(rule.getAction(), rule.isDryRun(), duplicateFile, originalFile, rule);

    }   
    
        public void applyAction(String action, boolean dryRun, FileEntry duplicateFile, FileEntry originalFile, FolderRule rule) {
        action = action.toUpperCase();

        try {
             switch (action) {
                case "NOTIFY":
                       logAction(rule, action, duplicateFile, "Duplicate found", dryRun);
                    break;
                case "SKIP":
                    logAction(rule, action, duplicateFile, "Skipped", dryRun);
                    break;    
                case "DELETE":   
                       if (!dryRun) {  
                        fileOps.delete(Paths.get(duplicateFile.getPath()));

                    }
                    logAction(rule, action, duplicateFile, "Deleted duplicate file", dryRun);
                    break;
                case "HARDLINK":
                    if (!dryRun) {


                        fileOps.hardlink(Paths.get(originalFile.getPath()), Paths.get(duplicateFile.getPath()));     

                       }   

                        logAction(rule, action, duplicateFile, "Hardlinked to original", dryRun);

                      break;
                case "QUARANTINE":

                    if (!dryRun) {
                        quarantineFile(duplicateFile, "DUPLICATE");
                      }  
                    logAction(rule, action, duplicateFile, "Quarantined duplicate file", dryRun);
                       break;
                  default:

                    log.warn("Unknown action {} for rule {}", action, rule != null ? rule.getId() : "MANUAL");
               }     
        } catch (Exception e) {     

            log.error("Failed to apply action {} to file {}: {}", action, duplicateFile.getPath(), e.getMessage());  
            logAction(rule, action, duplicateFile, "FAILED: " + e.getMessage(), dryRun);     
            }
    }

    private void quarantineFile(FileEntry fileEntry, String reason) throws IOException {

        Path source = Paths.get(fileEntry.getPath());
           if (!quarantineDir.toFile().exists()) {
            quarantineDir.toFile().mkdirs();
        }

        
        String safeName = UUID.randomUUID().toString() + "_" + source.getFileName().toString();

        Path target = quarantineDir.resolve(safeName);

        fileOps.move(source, target);

        QuarantineItem item = new QuarantineItem(); 
        item.setFileEntry(fileEntry);     
        item.setOriginalPath(fileEntry.getPath());
          item.setQuarantinePath(target.toString());
        item.setFullHash(fileEntry.getFullHash());    
        item.setReason(reason);
        // Default 30 days retention could be added here
        
        quarantineItemRepository.save(item);
     } 
   
    private void logAction(FolderRule rule, String action, FileEntry target, String result, boolean dryRun) {
          AuditLog logEntry = new AuditLog();

        logEntry.setActor(rule != null ? "RuleEngine (Rule " + rule.getId() + ")" : "System Default");
        logEntry.setAction(action);
          logEntry.setTargetPath(target.getPath());
          logEntry.setResult(result);

        logEntry.setDryRun(dryRun);   

    
           try {

               logEntry.setBeforeState(objectMapper.writeValueAsString(Map.of("hash", target.getFullHash(), "size", target.getSize())));
            logEntry.setAfterState(objectMapper.writeValueAsString(Map.of("status", result)));
        } catch (Exception e) {
            log.warn("Could not serialize state for audit log", e);
        }

 
        auditLogRepository.save(logEntry);
        log.info("Policy Audit: Action {}, Target {}, Result {} (DryRun: {})", action, target.getPath(), result, dryRun);
    }
}
