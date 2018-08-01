package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepo extends JpaRepository<FileEntry, Integer> {

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

//    @Transactional
//    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
//            "select cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
//            "union\n" +
//            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//            ")\n" +
//            "select cid,cname,ccrtdt,csize,cisdir,cparent,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?1) limit 1", nativeQuery = true)
//    ExtendedFileEntry findFileEntryByPath(String path);
//
//    @Transactional
//    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
//            "select cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
//            "union\n" +
//            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//            ")\n" +
//            "select cid,cname,ccrtdt,csize,cisdir,cparent,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?1)", nativeQuery = true)
//    List<ExtendedFileEntry> findFileEntriesByPath(String path);
//
//    @Transactional
//    @Query(value = "WITH RECURSIVE my_tree AS (\n" +
//            "select cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
//            "union\n" +
//            "select b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//            ")\n" +
//            "select cid,'..' cname,ccrtdt,csize,cisdir,cparent, coalesce(cfullpath,'/') cfullpath from my_tree where cfullpath=?1 "+
//            " union all "+
//            "select cid,cname,ccrtdt,csize,cisdir,cparent,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?1)", nativeQuery = true)
//    List<ExtendedFileEntry> findFileEntriesByPathWithParent(String path);

}
