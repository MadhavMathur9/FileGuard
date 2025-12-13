package com.fileguard.domain;

    import jakarta.persistence.*;

import lombok.Data;

   @Entity
@Table(name = "duplicate_group")    
   @Data
    public class DuplicateGroup {
    
    @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;


    @Column(name = "full_hash", nullable = false, unique = true, length = 64)
    private String fullHash;
    
    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "original_file_id")

       private FileEntry originalFile;

    @Column(name = "member_count", nullable = false)
       private int memberCount; 
  
    @Column(name = "wasted_bytes", nullable = false)

        private long wastedBytes;

}
