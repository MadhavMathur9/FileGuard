package com.fileguard.domain;

   import jakarta.persistence.*;

import lombok.Data;  

import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
  import org.hibernate.type.SqlTypes;

@Entity
    @Table(name = "audit_log")
@Data

  public class AuditLog {

    @Id

        @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    
    @CreationTimestamp
     @Column(updatable = false)

    private Instant ts;    

        @Column(nullable = false, length = 100)
      private String actor;
 
    @Column(nullable = false, length = 100)
    private String action;

    
    @Column(name = "target_path", nullable = false, length = 1024)   
    private String targetPath;


    @JdbcTypeCode(SqlTypes.JSON)  
    @Column(name = "before_state", columnDefinition = "jsonb")
     private String beforeState;

        @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "jsonb")     

    private String afterState;

    @Column(nullable = false, length = 50)
        private String result;

    @Column(name = "dry_run", nullable = false)

    private boolean dryRun = false;
}
