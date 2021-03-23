package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepo extends JpaRepository<Block, Integer> {

    @Query(value = "select * from tblock a where exists(select 1 from tfiletable b where a.cid = b.cblkid and cfileid = ?1)", nativeQuery = true)
    List<Block> findBlocksByFileId(Integer fileId);
}
