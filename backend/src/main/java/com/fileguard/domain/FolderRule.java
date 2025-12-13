    package com.fileguard.domain;

import jakarta.persistence.*;
import lombok.Data;


@Entity

@Table(name = "folder_rule")


    @Data
public class FolderRule {


    @Id  
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private WatchedFolder folder;

    @Column(nullable = false, length = 50)
    private String trigger; // e.g. "DUPLICATE"


    @Column(nullable = false, length = 50)    
    private String action; // e.g. "NOTIFY", "QUARANTINE", "HARDLINK", "DELETE", "SKIP"


    @Column(name = "dry_run", nullable = false)
    private boolean dryRun = false;   
 
    @Column(nullable = false)
    private boolean enabled = true;
}
