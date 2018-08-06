package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ExtendedFileRepo extends JpaRepository<ExtendedFileEntry, Integer> {

//    @Query("select f, f.cname as cfullpath from ExtendedFileEntry f where f.cname = ?1")
//    FileEntry findByName(String path);

//    List<FileEntry> findAll();

//    @Transactional
//    @Modifying
//    @Query("update FileEntry f set cright = cright + 2 where f.cright >= ?1")
//    void updateRight(int rightVal);
//
//    @Transactional
//    @Modifying
//    @Query("update FileEntry f set cleft = cleft + 2 where f.cleft > ?1")
//    void updateLeft(int rightVal);

//    @Transactional
//    @Modifying
//    @Query("delete from ExtendedFileEntry where cname = ?1 and cpath = ?2")
//    void deleteByName(String name, String path);

    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
            "union\n" +
            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent,b.clastlock, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
            ")\n" +
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cfullpath = ?1 limit 1", nativeQuery = true)
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="false") })
    ExtendedFileEntry findFileEntryByPath(String path);

    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
            "select cid, cast('..' as text) cname,ccrtdt,csize,cisdir,cparent,clastlock, coalesce(cname,'/') cfullpath, -1 corder from tfiles where cparent is null\n" +
            "union\n" +
            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent,b.clastlock, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath, (case when b.cisdir = 0 then 1 else 0 end) corder from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
            ")\n" +
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?1) order by corder",
            nativeQuery = true)
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="false") })
    List<ExtendedFileEntry> findFileEntriesByPath(String path);

    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock, coalesce(cname,'/') cfullpath, -1 corder from tfiles where cparent is null\n" +
            "union\n" +
            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent,b.clastlock, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath, (case when b.cisdir = 0 then 1 else 0 end) corder from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
            ")\n" +
            // the 0-cid value here is mandatory to make cname displayed as ..
            // it seems that jpa have cached the value of name and does not let us to override it
            "select cast(0-cid as int) cid, cast('..' as text) cname,ccrtdt,csize,cisdir,cparent,clastlock, coalesce(cfullpath,'/') cfullpath from my_tree where cfullpath=?1\n"
            +" union all \n"+
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?1)"
            , nativeQuery = true)
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="false") })
    List<ExtendedFileEntry> findFileEntriesByPathWithParent(String path);

    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
            "select cid,cname,ccrtdt,csize,cisdir,cparent,clastlock, coalesce(cname,'/') cfullpath, -1 corder from tfiles where cparent is null\n" +
            "union\n" +
            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent,b.clastlock, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath, (case when b.cisdir = 0 then 1 else 0 end) corder from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
            ")\n" +
            // the 0-cid value here is mandatory to make cname displayed as ..
            // it seems that jpa have cached the value of name and does not let us to override it
            "select exists(select 1 from my_tree where cfullpath=?1)"
            , nativeQuery = true)
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value ="false") })
    boolean existsByPath(String path);

}
