
package com.fileguard.controller;


import com.fileguard.domain.IntegrityAlert;


import com.fileguard.integrity.IntegrityService;

import com.fileguard.repository.IntegrityAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;     
import org.springframework.web.bind.annotation.*;  

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IntegrityController {   

    private final IntegrityService integrityService;

    private final IntegrityAlertRepository alertRepository;

    @PostMapping("/folders/{id}/baseline")
    public ResponseEntity<?> createBaseline(@PathVariable Long id) {

        integrityService.createBaseline(id);
        return ResponseEntity.ok().build();
      }

       @GetMapping("/alerts")
    public ResponseEntity<Page<IntegrityAlert>> getAlerts(

            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String state,

            Pageable pageable) {
        
          Page<IntegrityAlert> alerts;
        if (folderId != null) {

            alerts = alertRepository.findByFolderId(folderId, pageable);
        } else if (state != null) {
            alerts = alertRepository.findByState(state, pageable);
        } else {
                alerts = alertRepository.findAll(pageable);
           }
        
        return ResponseEntity.ok(alerts);     
    }

        @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id) {
         integrityService.acknowledgeAlert(id); 
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/alerts/{id}/accept")

    public ResponseEntity<?> acceptAlert(@PathVariable Long id) {   
            integrityService.acceptAlert(id);

        return ResponseEntity.ok().build();
    }
   }
