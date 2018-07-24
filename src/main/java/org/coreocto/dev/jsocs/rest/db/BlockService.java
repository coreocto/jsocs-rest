package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.Block;

import java.util.List;

public interface BlockService {

    void create(String name, long size, int owner);
    void create(String name, long size, int owner, String remoteId);
    void create(String name, long size, int owner, String remoteId, String directLink);

    List<Block> getBlocks(boolean includeUsed, boolean includeUnused);
    List<Block> getByFileId(int fileId);

    Block getById(int blockId);
    Block getByName(String name);

    void update(int blockId, String remoteId, String directLink);
    void update(int blockId, boolean inuse);

    void deleteById(int blockId);
    void deleteAllBlocks();
}
