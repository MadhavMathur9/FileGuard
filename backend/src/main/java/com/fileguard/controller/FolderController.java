
package com.fileguard.controller;

import com.fileguard.domain.FolderRule;     
import com.fileguard.domain.WatchedFolder;  
import com.fileguard.repository.FolderRuleRepository;
    import com.fileguard.repository.WatchedFolderRepository;

import com.fileguard.watcher.WatcherService;
import com.fileguard.reconcile.ReconciliationService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
  import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; 

  import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@RestController
 @RequestMapping("/api/v1/folders")

@RequiredArgsConstructor 
@Slf4j
 public class FolderController {

    private final WatchedFolderRepository repository;
    private final FolderRuleRepository folderRuleRepository;   
      private final WatcherService watcherService;
    private final ReconciliationService reconciliationService;

    @GetMapping
    public List<WatchedFolder> getAllFolders() {
           return repository.findAll();   

    }
   
    @PostMapping
    public WatchedFolder addFolder(@RequestBody WatchedFolder folder) { 
        WatchedFolder saved = repository.save(folder);
        if (saved.isRecursive()) {

              try {
                Path path = Paths.get(saved.getPath());
                watcherService.registerAll(path);

            } catch (IOException e) {

                 log.error("Failed to register watcher for path: {}", saved.getPath(), e);

            }
        }
           return saved;   
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WatchedFolder> updateFolder(@PathVariable Long id, @RequestBody WatchedFolder updates) {
        return repository.findById(id).map(folder -> {

            if (updates.getPath() != null) folder.setPath(updates.getPath());   
            if (updates.getMode() != null) folder.setMode(updates.getMode());
            if (updates.getExcludeGlobs() != null) folder.setExcludeGlobs(updates.getExcludeGlobs());
            // simplified updates
            return ResponseEntity.ok(repository.save(folder));    
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")  
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {

        if (repository.existsById(id)) {     
              repository.deleteById(id);

            return ResponseEntity.noContent().build();     

        }
        return ResponseEntity.notFound().build();

    }    

    @GetMapping("/{id}/rules")
     public ResponseEntity<List<FolderRule>> getFolderRules(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();

        }  
        return ResponseEntity.ok(folderRuleRepository.findByFolderId(id));
    }     

    @PutMapping("/{id}/rules")
    public ResponseEntity<List<FolderRule>> setFolderRules(@PathVariable Long id, @RequestBody List<FolderRule> rules) {

         return repository.findById(id).map(folder -> {

            // Delete existing   
               List<FolderRule> existing = folderRuleRepository.findByFolderId(id);     

               folderRuleRepository.deleteAll(existing);
            

             // Save new
            rules.forEach(r -> { 
                r.setId(null);
                r.setFolder(folder);     

              });
            return ResponseEntity.ok(folderRuleRepository.saveAll(rules));
        }).orElse(ResponseEntity.notFound().build());
     }
   
    @PostMapping("/{id}/scan")     

    public ResponseEntity<?> triggerScan(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean deep) {
        reconciliationService.reconcileFolderAsync(id, deep);
        return ResponseEntity.ok().build();   
    } 
}

