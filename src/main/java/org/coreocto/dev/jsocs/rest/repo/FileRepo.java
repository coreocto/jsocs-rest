package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.SQLUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface FileRepo extends JpaRepository<FileEntry, Integer> {

    @Query("select f, f.cname as cfullpath from FileEntry f where f.cname = ?1")
    FileEntry findByName(String path);

    List<FileEntry> findAll();

//    @Transactional
//    @Modifying
//    @Query("update FileEntry f set cright = cright + 2 where f.cright >= ?1")
//    void updateRight(int rightVal);
//
//    @Transactional
//    @Modifying
//    @Query("update FileEntry f set cleft = cleft + 2 where f.cleft > ?1")
//    void updateLeft(int rightVal);

    @Transactional
    @Modifying
    @Query("delete from FileEntry where cname = ?1 and cpath = ?2")
    void deleteByName(String name, String path);

}
