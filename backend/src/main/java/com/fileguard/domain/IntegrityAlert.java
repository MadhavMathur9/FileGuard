   package com.fileguard.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
    
@Entity

@Table(name = "integrity_alert")
@Data   
public class IntegrityAlert {
       @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)    
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false) 

    @JoinColumn(name = "folder_id", nullable = false)

    private WatchedFolder folder;


     @Column(nullable = false, length = 20)  
       private String type; // MODIFIED, ADDED, REMOVED, RENAMED
  

    @Column(nullable = false, length = 1024)
        private String path;

    @Column(name = "old_hash", length = 64)

    private String oldHash;

    @Column(name = "new_hash", length = 64)
       private String newHash;
    
    @Column(nullable = false, length = 30)


    private String state; // OPEN, ACKNOWLEDGED, ACCEPTED_AS_NEW_BASELINE

    @Column(name = "raised_at", nullable = false, updatable = false)
    private Instant raisedAt;

       @Column(name = "resolved_at")
      private Instant resolvedAt;


    @PrePersist
    public void prePersist() {

        if (raisedAt == null) {

            raisedAt = Instant.now();

        }    

           if (state == null) {
            state = "OPEN"; 
          }

    }
}
