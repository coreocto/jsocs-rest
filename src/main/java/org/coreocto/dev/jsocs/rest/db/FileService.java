package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.FileEntry;

import java.util.List;

public interface FileService {
    void create(String path, String name, long size);

    List<FileEntry> getFiles(String path, boolean includeSubDir);
    FileEntry getByName(String path, String name);
    FileEntry getById(int fileId);

    void deleteAllFiles();
    void deleteByPath(String path);
    void deleteByName(String path, String name);
    void deleteById(int id);
}
