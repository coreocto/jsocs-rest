package org.coreocto.dev.jsocs.rest.nio;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IRemoteStorage {
    List<Map<String,Object>> provision(File srcFile, List<String> blockNames) throws IOException;
    void update();
    void delete(long fileId) throws IOException;
    void download(String fileName, File targetFile) throws IOException;
    Map<String,Object> upload(File srcFile, String fileName) throws IOException;
    int getUserId();
}
