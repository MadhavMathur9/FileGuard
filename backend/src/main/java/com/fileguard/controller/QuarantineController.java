package com.fileguard.controller;

import com.fileguard.domain.QuarantineItem;

import com.fileguard.quarantine.QuarantineService;

import com.fileguard.repository.QuarantineItemRepository;    
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
   import org.springframework.web.bind.annotation.*;
    
import java.io.IOException;    
import java.util.List;
    
@RestController
@RequestMapping("/api/v1/quarantine")
@RequiredArgsConstructor

 public class QuarantineController {


    private final QuarantineItemRepository quarantineItemRepository;
    private final QuarantineService quarantineService;
 
    @GetMapping   
    public List<QuarantineItem> listQuarantineItems() {
        return quarantineItemRepository.findByRestoredAtIsNull();
        }

        @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreItem(@PathVariable Long id) { 
        try {     
            quarantineService.restore(id);     
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {     
            return ResponseEntity.badRequest().body(e.getMessage());  
        } catch (IOException e) {

               return ResponseEntity.internalServerError().body("Restore failed: " + e.getMessage());

        }     

    }


      @DeleteMapping("/{id}")     
    public ResponseEntity<?> purgeItem(@PathVariable Long id) {

        try {
            quarantineService.purge(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());   
            } catch (IOException e) {

                return ResponseEntity.internalServerError().body("Purge failed: " + e.getMessage());
        }
    }   
}
