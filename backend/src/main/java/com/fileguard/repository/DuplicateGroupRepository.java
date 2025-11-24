package com.fileguard.repository;
    
import com.fileguard.domain.DuplicateGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

    
@Repository
public interface DuplicateGroupRepository extends JpaRepository<DuplicateGroup, Long> {

    Optional<DuplicateGroup> findByFullHash(String fullHash);    

        @org.springframework.data.jpa.repository.Query("SELECT SUM(d.wastedBytes) FROM DuplicateGroup d")

    Long sumWastedBytes();

}   
