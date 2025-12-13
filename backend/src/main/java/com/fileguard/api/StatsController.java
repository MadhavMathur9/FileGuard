package com.fileguard.api;

import com.fileguard.repository.DuplicateGroupRepository; 
  import com.fileguard.repository.FileEntryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;

 
import java.util.Map;

 @RestController

@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor    
  public class StatsController {

    private final FileEntryRepository fileEntryRepository;

    private final DuplicateGroupRepository duplicateGroupRepository;


    @GetMapping   
    public Map<String, Object> getStats() {
        long totalFiles = fileEntryRepository.count();

            // Since we don't have a sum query easily available without a custom query, we could do:
        Long totalSize = fileEntryRepository.sumSize();
        if (totalSize == null) totalSize = 0L;
  
        long duplicateGroups = duplicateGroupRepository.count();
        Long wastedBytes = duplicateGroupRepository.sumWastedBytes();

        if (wastedBytes == null) wastedBytes = 0L;

        return Map.of(    
                "totalFiles", totalFiles,
                   "totalSize", totalSize,
                "duplicateGroups", duplicateGroups,    
                "wastedBytes", wastedBytes
            );     
     }
}
