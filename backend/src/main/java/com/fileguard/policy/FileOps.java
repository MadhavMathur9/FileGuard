package com.fileguard.policy;

import java.io.IOException;

import java.nio.file.Path;


public interface FileOps {     

    void delete(Path path) throws IOException;


    void move(Path source, Path target) throws IOException;
    void hardlink(Path existing, Path link) throws IOException;
    }
