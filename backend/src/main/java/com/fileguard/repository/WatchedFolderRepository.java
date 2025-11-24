package com.fileguard.repository;

import com.fileguard.domain.WatchedFolder;    

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository    
public interface WatchedFolderRepository extends JpaRepository<WatchedFolder, Long> {

}   

