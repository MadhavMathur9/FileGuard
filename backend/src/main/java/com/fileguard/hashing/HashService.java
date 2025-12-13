
package com.fileguard.hashing;

    import org.springframework.stereotype.Service;
    
import java.io.IOException;     
import java.io.InputStream;
 import java.nio.file.Files;    
import java.nio.file.Path;
import java.security.MessageDigest;

  import java.security.NoSuchAlgorithmException;

@Service

  public class HashService {
 
    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB chunk for constant memory 
    private static final int PARTIAL_HASH_SIZE = 4096; // 4 KB

    public String computePartialHash(Path path) throws IOException {   
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");


               byte[] buffer = new byte[PARTIAL_HASH_SIZE];
            int bytesRead = is.read(buffer);
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead);
             }
             return bytesToHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String computeFullHash(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) { 
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[CHUNK_SIZE];

            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {    
                digest.update(buffer, 0, bytesRead);
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }


       private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); 
        }

        return sb.toString();
     }
}
