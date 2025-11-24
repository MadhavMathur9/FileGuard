package com.fileguard.repository;
  
import com.fileguard.domain.BaselineEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;    

  import java.util.List;
import java.util.Optional;

  @Repository  
public interface BaselineEntryRepository extends JpaRepository<BaselineEntry, Long> {
    List<BaselineEntry> findByFolderId(Long folderId);    
    Optional<BaselineEntry> findByFolderIdAndPath(Long folderId, String path);
    List<BaselineEntry> findByFolderIdAndFullHash(Long folderId, String fullHash);
}
