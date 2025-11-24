package com.fileguard.repository;   
   
import com.fileguard.domain.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
    import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
 import org.springframework.data.domain.Pageable;
 
@Repository
public interface FileEntryRepository extends JpaRepository<FileEntry, Long> {

    Optional<FileEntry> findByFolderIdAndPath(Long folderId, String path);   

    List<FileEntry> findBySize(long size);

    List<FileEntry> findBySizeAndPartialHash(long size, String partialHash);
    List<FileEntry> findByFolderId(Long folderId);


       @Query("SELECT f.fullHash FROM FileEntry f WHERE f.fullHash IS NOT NULL GROUP BY f.fullHash HAVING COUNT(f) > 1")
    Page<String> findDuplicateHashes(Pageable pageable);

       List<FileEntry> findByFullHash(String fullHash);   

    List<FileEntry> findByFullHashIn(List<String> hashes);

 
    @Query("SELECT SUM(f.size) FROM FileEntry f")
    Long sumSize();
}     
