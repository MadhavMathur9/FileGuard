package com.fileguard.repository;

import com.fileguard.domain.FolderRule;
import org.springframework.data.jpa.repository.JpaRepository;

    import org.springframework.stereotype.Repository;
 
  import java.util.List;


@Repository

public interface FolderRuleRepository extends JpaRepository<FolderRule, Long> {  

       List<FolderRule> findByFolderId(Long folderId);
    List<FolderRule> findByFolderIdAndEnabledTrue(Long folderId);
}    

