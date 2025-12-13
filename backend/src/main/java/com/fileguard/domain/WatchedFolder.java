package com.fileguard.domain;

   
   import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;

 @Entity
@Table(name = "watched_folder")
@Data
public class WatchedFolder {
    @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1024)
       private String path;
   
    @Column(nullable = false)
    private boolean recursive = true;  

        @Column(nullable = false, length = 50)

        private String mode;
    
    @Column(name = "exclude_globs", columnDefinition = "TEXT")
     private String excludeGlobs;    
    
        @CreationTimestamp
    @Column(name = "created_at", updatable = false)   
    private OffsetDateTime createdAt;
}
