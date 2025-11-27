    package com.fileguard.pipeline;     

import com.fileguard.events.EventType;
import com.fileguard.events.FileEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;    
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration; 
import java.time.Instant;
import java.util.Iterator;   

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;   

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

import static java.nio.file.StandardWatchEventKinds.*;
 
@Service
@Slf4j

  @RequiredArgsConstructor   
   public class EventIngester {
    
       private final Map<Path, PendingEvent> pendingEvents = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();  
  
    private final HashWorkerPool hashWorkerPool;


     @Value("${fileguard.pipeline.debounce-ms:500}")

    private long debounceMs;    

    @Value("${fileguard.pipeline.stability-ms:2000}")
      private long stabilityMs;    

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::processPendingEvents, debounceMs, debounceMs, TimeUnit.MILLISECONDS);
    }
 
    @PreDestroy
       public void cleanup() {    
        scheduler.shutdown();
    }

   
      public void ingest(Path path, WatchEvent.Kind<?> kind) {
        EventType type = mapKind(kind); 
           if (type == null) return;

        pendingEvents.compute(path, (p, pending) -> {
            if (pending == null) {
                return new PendingEvent(path, type);
            } else {
                    pending.updateEvent(type);
                return pending;

              }    
        });
    }


    private void processPendingEvents() {
        Instant now = Instant.now();
        Iterator<Map.Entry<Path, PendingEvent>> it = pendingEvents.entrySet().iterator();

        while (it.hasNext()) {     
            Map.Entry<Path, PendingEvent> entry = it.next();
            PendingEvent pending = entry.getValue();
            Path path = entry.getKey();

                long msSinceLastEvent = Duration.between(pending.getLastEventTime(), now).toMillis();

            if (pending.getUltimateType() == EventType.DELETED) {

                  // If DELETED, wait for debounce window to ensure no rapid recreation

                if (msSinceLastEvent >= debounceMs) {
                    it.remove();

                    dispatch(FileEvent.builder()

                            .path(path)
                            .type(EventType.DELETED) 
                            .build());
                }
            } else {
                // CREATED or MODIFIED require stability check

                try {

                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);     


                     if (attrs.isDirectory()) {     
                        // We only process files in the hashing pipeline
                        it.remove();
                           continue;
                    }
  
                      pending.updateStability(attrs.size(), attrs.lastModifiedTime().toMillis()); 
 
                    if (pending.getStableSince() != null) { 

                        long msStable = Duration.between(pending.getStableSince(), now).toMillis();

                        if (msStable >= stabilityMs) {

                            it.remove();
                            dispatch(FileEvent.builder()
                                    .path(path)
                                    .type(pending.getUltimateType())
                                    .size(attrs.size())

                                    .mtime(attrs.lastModifiedTime().toInstant())   
                                    .build());
                        }   
                     }
                } catch (NoSuchFileException e) {
                       // File was deleted before reaching stability
                    pending.updateEvent(EventType.DELETED);

                } catch (IOException e) {
                    log.error("Error reading attributes for {}", path, e);
                 }

              }     
        }
        }
  
        private void dispatch(FileEvent event) {
            log.info("Pipeline Dispatched: {}", event);
        hashWorkerPool.submit(event);    
    }

    private EventType mapKind(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) return EventType.CREATED;

         if (kind == ENTRY_MODIFY) return EventType.MODIFIED;  

        if (kind == ENTRY_DELETE) return EventType.DELETED;   
        return null; 
    }  
}
