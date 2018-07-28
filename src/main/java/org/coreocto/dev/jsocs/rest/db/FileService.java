package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;

import java.util.List;

public interface FileService {
    void create(String path, String name, long size);

    List<ExtendedFileEntry> getFiles(String path, boolean includeSubDir);
//    List<FileEntry> getFiles(int parentId, boolean includeSubDir);
//    FileEntry getByName(String path, String name);
//    FileEntry getById(int fileId);
    FileEntry getByPath(String path);
    FileEntry getByParentAndName(int parentId, String name);

//    void deleteAllFiles();
//    void deleteByPath(String path);
//    void deleteByName(String path, String name);
    void deleteById(int id);
}
