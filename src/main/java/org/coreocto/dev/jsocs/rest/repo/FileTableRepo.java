package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.FileTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface FileTableRepo extends JpaRepository<FileTable, Integer> {

    @Query("select ft from FileTable ft where cfileid = ?1")
    List<FileTable> findByCfileid(int fileId);

    @Query("select ft from FileTable ft where cblkid = ?1")
    List<FileTable> findByCblkid(int blockId);

    @Transactional
    @Modifying
    @Query("delete from FileTable where cfileid = ?1")
    void deleteByCfileid(int fileId);
}
