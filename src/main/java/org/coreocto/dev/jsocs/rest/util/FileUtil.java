package org.coreocto.dev.jsocs.rest.util;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUtil {

    public String getParentPath(String fullPath){
        Path p = Paths.get(fullPath);
        return p.getParent().toString();
    }

    public String getFileName(String fullPath){
        Path p = Paths.get(fullPath);
        return p.getFileName().toString();
    }
}
