package com.fileguard.controller;
  
import com.fileguard.domain.AuditLog;
import com.fileguard.repository.AuditLogRepository;
    import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;  
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1/audit")   
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditLogRepository auditLogRepository;

    @GetMapping
        public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable); 
    } 
}

