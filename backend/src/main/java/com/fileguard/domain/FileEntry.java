package com.fileguard.domain;
   
  import jakarta.persistence.*;  
import lombok.Data;


import java.time.OffsetDateTime;  


@Entity
@Table(name = "file_entry")   
   @Data
public class FileEntry {     

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_id", nullable = false)
    private Long folderId;   

    @Column(nullable = false, length = 1024)
    private String path;    
  

    @Column(nullable = false)
    private long size;
    
    @Column(nullable = false)
    private OffsetDateTime mtime;

       @Column(name = "partial_hash", length = 64)
    private String partialHash;

    @Column(name = "full_hash", length = 64)
        private String fullHash;

     @Column(nullable = false, length = 50)

    private String status;
 
    @Column(name = "first_seen_at", insertable = false, updatable = false)

    private OffsetDateTime firstSeenAt;
 
    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;
 }
