package com.fileguard.policy;    

    import org.springframework.stereotype.Component;


import java.io.IOException;
import java.nio.file.Files;    
import java.nio.file.Path;

import java.nio.file.StandardCopyOption;
 
@Component
public class RealFileOps implements FileOps {
   
    @Override

    public void delete(Path path) throws IOException {
        Files.deleteIfExists(path);
    }  
 
    @Override
    public void move(Path source, Path target) throws IOException {    

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

    } 


    @Override

    public void hardlink(Path existing, Path link) throws IOException {

        Files.deleteIfExists(link); // Ensure target is clear before linking
         Files.createLink(link, existing);     

    }    
}
