package com.fileguard.events;  

import lombok.Builder;

 import lombok.Data;


import java.nio.file.Path;
import java.time.Instant;

@Data     
@Builder
public class FileEvent {
        private Path path;  
    private EventType type;
    private long size;
    private Instant mtime;    

    private Path oldPath; // Used only for MOVED events
   }
