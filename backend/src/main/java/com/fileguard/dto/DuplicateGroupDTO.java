package com.fileguard.dto;
 
import com.fileguard.domain.DuplicateGroup;
import com.fileguard.domain.FileEntry;   

    import lombok.AllArgsConstructor;

import lombok.Builder;
    import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
   import java.util.stream.Collectors;  

@Data
@Builder

@NoArgsConstructor

@AllArgsConstructor
public class DuplicateGroupDTO {
      private Long id;
       private String fullHash;   
    private long size;
      private Long originalFileId;
    private int memberCount;

    private long wastedBytes;
    private List<FileEntryDTO> files;

    public static DuplicateGroupDTO from(DuplicateGroup group, List<FileEntry> entries) {
        if (group == null || entries == null || entries.isEmpty()) {
            return null;
        }

        return DuplicateGroupDTO.builder()

                 .id(group.getId())  

                .fullHash(group.getFullHash())  
                .size(entries.get(0).getSize()) 

                .originalFileId(group.getOriginalFile() != null ? group.getOriginalFile().getId() : null)   
                .memberCount(group.getMemberCount())
                .wastedBytes(group.getWastedBytes())  
                .files(entries.stream().map(FileEntryDTO::from).collect(Collectors.toList()))
                .build();    
    }

 }
