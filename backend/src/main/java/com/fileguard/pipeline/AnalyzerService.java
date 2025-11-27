package com.fileguard.pipeline;

import com.fileguard.domain.DuplicateGroup;
  import com.fileguard.domain.FileEntry;

import com.fileguard.policy.PolicyEngine;
import com.fileguard.repository.DuplicateGroupRepository;

import com.fileguard.repository.FileEntryRepository;

import lombok.RequiredArgsConstructor;   
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

 import com.fileguard.api.SseService;
import java.util.Map;


@Service 
@Slf4j

@RequiredArgsConstructor    
public class AnalyzerService {



     private final DuplicateGroupRepository duplicateGroupRepository;
    private final FileEntryRepository fileEntryRepository;
    private final PolicyEngine policyEngine;
    private final SseService sseService;


    @Transactional
    public void analyzeDuplicates(String fullHash, FileEntry newlyHashedEntry) {
            if (fullHash == null) {  
                return;
        }

        List<FileEntry> entries = fileEntryRepository.findByFullHash(fullHash);
        Optional<DuplicateGroup> existingGroup = duplicateGroupRepository.findByFullHash(fullHash);   

        if (entries.size() <= 1) {

            // Not a duplicate group anymore, or never was

            existingGroup.ifPresent(duplicateGroupRepository::delete);

            return;
        }     
  
        // It is a duplicate group
          DuplicateGroup group = existingGroup.orElseGet(() -> {
            DuplicateGroup newGroup = new DuplicateGroup();
            newGroup.setFullHash(fullHash);

            // Default original is the first one
            newGroup.setOriginalFile(entries.get(0));

              return newGroup;
        });

            // Ensure original is still valid
        if (group.getOriginalFile() == null || !entries.contains(group.getOriginalFile())) {   

            group.setOriginalFile(entries.get(0));
        }    

        long fileSize = entries.get(0).getSize();   
        group.setMemberCount(entries.size());  
        group.setWastedBytes(fileSize * (entries.size() - 1));

        duplicateGroupRepository.save(group);
        log.info("Updated DuplicateGroup for hash {}, {} members, {} wasted bytes", fullHash, group.getMemberCount(), group.getWastedBytes());  

         sseService.publishEvent("duplicate.found", Map.of(
                "hash", fullHash,

                "count", group.getMemberCount()

        ));

        // Evaluate policy for the newly hashed file if it is a duplicate (not the original)
        if (newlyHashedEntry != null && entries.size() > 1 && !newlyHashedEntry.getId().equals(group.getOriginalFile().getId())) {
             policyEngine.evaluateDuplicate(newlyHashedEntry, group.getOriginalFile());
        }
    }

}  
