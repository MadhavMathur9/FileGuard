package com.fileguard.domain;

import jakarta.persistence.*;
import lombok.Data;     

import java.time.OffsetDateTime;
import java.time.Instant;

    
  @Entity
@Table(name = "baseline_entry", uniqueConstraints = {
       @UniqueConstraint(columnNames = {"folder_id", "path"})

})
@Data
public class BaselineEntry {

        @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
     @JoinColumn(name = "folder_id", nullable = false)
    private WatchedFolder folder;

    @Column(nullable = false, length = 1024)
    private String path;

    @Column(name = "full_hash", length = 64)
    private String fullHash; 


    @Column(nullable = false)   
    private long size;   

    @Column(nullable = false)
      private OffsetDateTime mtime;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;
}   
