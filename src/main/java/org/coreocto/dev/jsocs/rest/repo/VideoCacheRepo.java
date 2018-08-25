package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.VideoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoCacheRepo extends JpaRepository<VideoCache,Integer> {

    @Query(value = "select f from VideoCache f where f.cfileid = ?1")
    VideoCache findByFileId(Integer cid);
}
