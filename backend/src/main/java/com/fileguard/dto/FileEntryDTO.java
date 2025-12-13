package com.fileguard.dto;


import com.fileguard.domain.FileEntry;

import lombok.AllArgsConstructor; 

import lombok.Builder;

import lombok.Data;   
import lombok.NoArgsConstructor;
    
import java.time.OffsetDateTime;     


@Data
@Builder

@NoArgsConstructor

@AllArgsConstructor
public class FileEntryDTO {
    private Long id;
    private Long folderId;

       private String path;

    private long size; 

      private OffsetDateTime mtime;

    private String partialHash;
    private String fullHash;
    private OffsetDateTime firstSeenAt;
       private OffsetDateTime lastSeenAt;
    
    public static FileEntryDTO from(FileEntry entry) {
        if (entry == null) {
            return null;


        }   
        return FileEntryDTO.builder()    
                .id(entry.getId())
                .folderId(entry.getFolderId())

                .path(entry.getPath())
                .size(entry.getSize())  
                .mtime(entry.getMtime())
                .partialHash(entry.getPartialHash())

                 .fullHash(entry.getFullHash())
                .firstSeenAt(entry.getFirstSeenAt())
                .lastSeenAt(entry.getLastSeenAt())
                .build();
       }
}

