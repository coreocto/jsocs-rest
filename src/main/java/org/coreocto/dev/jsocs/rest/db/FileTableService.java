package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.FileTable;

import java.util.List;

public interface FileTableService {
    void create(int fileId, int blockId);

    List<FileTable> getByFileId(int fileId);

    void delete(int fileId, int blockId);

    void deleteByFileId(int fileId);
}
