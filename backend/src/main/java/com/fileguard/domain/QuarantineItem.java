
package com.fileguard.domain;   

 

    import jakarta.persistence.*;
    import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

    import java.time.Instant;

@Entity
@Table(name = "quarantine_item")   
@Data

public class QuarantineItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;     

       @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "file_entry_id", nullable = true)
        private FileEntry fileEntry;  
   
    @Column(name = "original_path", nullable = false, length = 1024)

    private String originalPath;

       @Column(name = "quarantine_path", nullable = false, length = 1024)
    private String quarantinePath;

    @Column(name = "full_hash", nullable = false, length = 64)
    private String fullHash;   


    @Column(nullable = false, columnDefinition = "TEXT")
       private String reason;

    @CreationTimestamp
        @Column(name = "quarantined_at", updatable = false)
    private Instant quarantinedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "restored_at")
    private Instant restoredAt;
}     
