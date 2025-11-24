package com.fileguard.repository;

import com.fileguard.domain.QuarantineItem;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


 import java.time.Instant;
  import java.util.List;     
    
@Repository    
public interface QuarantineItemRepository extends JpaRepository<QuarantineItem, Long> {
    List<QuarantineItem> findByExpiresAtBeforeAndRestoredAtIsNull(Instant now);
    List<QuarantineItem> findByRestoredAtIsNull();
}
