package com.fileguard.repository;

import com.fileguard.domain.AuditLog;  
import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;
    
  import java.util.List;

  
@Repository
  public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTargetPathOrderByTsDesc(String targetPath);
}  

