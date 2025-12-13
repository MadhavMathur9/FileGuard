
package com.fileguard.api;


 import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
    
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
   
@Service

@Slf4j

public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();    

    public SseEmitter createEmitter() {  

           SseEmitter emitter = new SseEmitter(0L); // no timeout
           this.emitters.add(emitter);  


        emitter.onCompletion(() -> {
            log.debug("SSE Emitter completed");

            this.emitters.remove(emitter);
         });
        emitter.onTimeout(() -> {
              log.debug("SSE Emitter timed out");
            emitter.complete();    

            this.emitters.remove(emitter);


        });
        emitter.onError(e -> {  

                log.debug("SSE Emitter error", e);

            emitter.completeWithError(e);
            this.emitters.remove(emitter);     

        });


        // Send an initial event to establish connection

        try { 

            emitter.send(SseEmitter.event().name("connected").data("true"));
        } catch (IOException e) {
             log.debug("Failed to send initial SSE event", e);
        }
    
        return emitter;

    }

    public void publishEvent(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        this.emitters.forEach(emitter -> {
            try {


                 emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });

        this.emitters.removeAll(deadEmitters);     

    }
}
