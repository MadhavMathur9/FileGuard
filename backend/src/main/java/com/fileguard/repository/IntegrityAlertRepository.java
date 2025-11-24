package com.fileguard.repository;

import com.fileguard.domain.IntegrityAlert;

    import org.springframework.data.domain.Page;  
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;


import java.util.Optional;   

@Repository
public interface IntegrityAlertRepository extends JpaRepository<IntegrityAlert, Long> {
    Page<IntegrityAlert> findByFolderId(Long folderId, Pageable pageable);

        Page<IntegrityAlert> findByState(String state, Pageable pageable);
    Optional<IntegrityAlert> findByFolderIdAndPathAndState(Long folderId, String path, String state);

}
