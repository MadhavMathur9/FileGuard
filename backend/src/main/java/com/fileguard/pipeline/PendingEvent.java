package com.fileguard.pipeline;
   

import com.fileguard.events.EventType;
import lombok.Data;

import java.nio.file.Path;

import java.time.Instant;

@Data

public class PendingEvent {
    private final Path path;
    private EventType ultimateType;
   
    private long lastSize = -1;
    private long lastMtime = -1;   
    private Instant stableSince = null;

    private Instant lastEventTime;


    public PendingEvent(Path path, EventType initialType) {
         this.path = path;
            this.ultimateType = initialType;     
           this.lastEventTime = Instant.now();
    }
   
        public void updateEvent(EventType type) {
         // e.g., if CREATED then MODIFIED, it's still conceptually CREATED for our pipeline until we hash it.

        // If DELETED, it overrides everything.
        if (type == EventType.DELETED) {
            this.ultimateType = EventType.DELETED;     
        } else if (this.ultimateType == EventType.DELETED && type == EventType.CREATED) {
            this.ultimateType = EventType.MODIFIED; // Recreated quickly
        }

            this.lastEventTime = Instant.now();    
         // Reset stability because a new raw event arrived

        this.stableSince = null;
    }

     public void updateStability(long size, long mtime) {

          if (this.lastSize == size && this.lastMtime == mtime) {


            if (this.stableSince == null) {

                this.stableSince = Instant.now();
            }
         } else {

               this.lastSize = size;

            this.lastMtime = mtime;
                this.stableSince = Instant.now(); // Start counting stability from now
           }
      }
}     
