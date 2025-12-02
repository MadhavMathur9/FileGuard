package com.fileguard.controller;


import com.fileguard.domain.DuplicateGroup;
   import com.fileguard.domain.FileEntry;
   import com.fileguard.domain.FileEntry;

import com.fileguard.dto.ActionRequestDTO;
import com.fileguard.dto.DuplicateGroupDTO;     
    import com.fileguard.policy.PolicyEngine;   
import com.fileguard.repository.DuplicateGroupRepository;
import com.fileguard.repository.FileEntryRepository;
   import lombok.RequiredArgsConstructor;     
import lombok.extern.slf4j.Slf4j; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl; 
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;     
   import org.springframework.web.bind.annotation.RestController;


import java.util.List;

import java.util.Map;  
import java.util.stream.Collectors;    

    
@Slf4j

@RestController
@RequestMapping("/api/v1/duplicates")
@RequiredArgsConstructor
  public class DuplicateController {

    private final FileEntryRepository fileEntryRepository;
        private final DuplicateGroupRepository duplicateGroupRepository;
        private final PolicyEngine policyEngine;
    private final com.fileguard.pipeline.AnalyzerService analyzerService;
   

    @GetMapping
       public ResponseEntity<Page<DuplicateGroupDTO>> getDuplicates(Pageable pageable) {
         log.info("Fetching duplicate groups, page: {}", pageable);

        
        // 1. Fetch paginated hashes that have duplicates 
        Page<String> duplicateHashesPage = fileEntryRepository.findDuplicateHashes(pageable);   
        
         if (duplicateHashesPage.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

          // 2. Fetch the corresponding DuplicateGroups and FileEntries
        List<String> hashes = duplicateHashesPage.getContent();   
        List<DuplicateGroup> groups = hashes.stream()

                .map(duplicateGroupRepository::findByFullHash)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)     

                    .collect(Collectors.toList());
  
        List<FileEntry> entries = fileEntryRepository.findByFullHashIn(hashes);

        // 3. Group files by hash

         Map<String, List<FileEntry>> groupedByHash = entries.stream() 
                .collect(Collectors.groupingBy(FileEntry::getFullHash));

        // 4. Convert to DTOs     
        List<DuplicateGroupDTO> dtos = groups.stream()   
                .map(group -> DuplicateGroupDTO.from(group, groupedByHash.get(group.getFullHash())))
                 .collect(Collectors.toList());
 
        Page<DuplicateGroupDTO> resultPage = new PageImpl<>(dtos, pageable, duplicateHashesPage.getTotalElements());
    
        return ResponseEntity.ok(resultPage);
    }
  
    @GetMapping("/{groupId}")

    public ResponseEntity<DuplicateGroupDTO> getGroupDetails(@PathVariable Long groupId) {
          return duplicateGroupRepository.findById(groupId)
                .map(group -> {
                    List<FileEntry> entries = fileEntryRepository.findByFullHash(group.getFullHash());
                    return ResponseEntity.ok(DuplicateGroupDTO.from(group, entries));
                })
                .orElse(ResponseEntity.notFound().build());

      }
   
       @PostMapping("/{groupId}/original/{fileId}")
    public ResponseEntity<DuplicateGroupDTO> setOriginalFile(@PathVariable Long groupId, @PathVariable Long fileId) {    
        DuplicateGroup group = duplicateGroupRepository.findById(groupId).orElse(null);
        if (group == null) { 


            return ResponseEntity.notFound().build();

          }

   
        FileEntry fileEntry = fileEntryRepository.findById(fileId).orElse(null);
        if (fileEntry == null || !fileEntry.getFullHash().equals(group.getFullHash())) {
             return ResponseEntity.badRequest().build();
        }

        group.setOriginalFile(fileEntry);
        duplicateGroupRepository.save(group);


        List<FileEntry> entries = fileEntryRepository.findByFullHash(group.getFullHash());
        return ResponseEntity.ok(DuplicateGroupDTO.from(group, entries));
    }


        @PostMapping("/{groupId}/apply")

    public ResponseEntity<?> applyAction(@PathVariable Long groupId, @RequestBody ActionRequestDTO request) {
         DuplicateGroup group = duplicateGroupRepository.findById(groupId).orElse(null);

        if (group == null) {
            return ResponseEntity.notFound().build(); 
        }


           FileEntry original = group.getOriginalFile();
        if (original == null) {

            return ResponseEntity.badRequest().body("Original file not set for group");    
        }


        List<FileEntry> entries = fileEntryRepository.findByFullHash(group.getFullHash());
            int applied = 0;   
        
        for (FileEntry entry : entries) {    

            if (!entry.getId().equals(original.getId())) {
                policyEngine.applyAction(request.getAction(), request.isDryRun(), entry, original, null);
                if (!request.isDryRun() && ("DELETE".equalsIgnoreCase(request.getAction()) || "QUARANTINE".equalsIgnoreCase(request.getAction()))) {
                    fileEntryRepository.delete(entry);

                }
                applied++;   

            }
        }
        
          if (!request.isDryRun() && ("DELETE".equalsIgnoreCase(request.getAction()) || "QUARANTINE".equalsIgnoreCase(request.getAction()))) {
            analyzerService.analyzeDuplicates(group.getFullHash(), null);
            }   
        
           return ResponseEntity.ok(Map.of("applied", applied, "action", request.getAction(), "dryRun", request.isDryRun()));

    }    

}
