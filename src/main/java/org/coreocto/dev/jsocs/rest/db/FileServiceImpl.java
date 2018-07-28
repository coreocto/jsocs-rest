package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.NamedNativeQuery;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void create(String path, String name, long size) {
        jdbcTemplate.update("insert into tfiles(cpath,cname,ccrtdt,csize) values(?,?,CURRENT_TIMESTAMP,?)", path, name, size);
    }

//    @Override
//    public void deleteByName(String path, String name) {
//        jdbcTemplate.update("delete from tfiles where cname = ? and cpath = ?", name, path);
//    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update("delete from tfiles where cid = ?", id);
    }

    @Override
    public FileEntry getByPath(String path){
        String sql = "WITH RECURSIVE my_tree AS (\n" +
                "\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
                "\t\t\tunion\n" +
                "\t\t\tselect b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
                "\t\t\t)\n";
        sql+="\t\t\tselect cid, cname,ccrtdt,csize,cisdir,cparent,cfullpath from my_tree where cfullpath = ?";
        return (FileEntry) jdbcTemplate.queryForObject(sql, new Object[]{path}, new BeanPropertyRowMapper(FileEntry.class));
    }

    @Override
    public List<ExtendedFileEntry> getFiles(String path, boolean includeSubDir) {


//        if (Constant.PATH_SEP.equals(path)) {
        String sql = "WITH RECURSIVE my_tree AS (\n" +
                "\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
                "\t\t\tunion\n" +
                "\t\t\tselect b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, (case when aaa.cfullpath = '/' then '' else aaa.cfullpath end)||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
                "\t\t\t)\n";
        if (!Constant.PATH_SEP.equals(path)) {
            sql+="select cid,'..' cname,ccrtdt,csize,cisdir,cparent, coalesce(cfullpath,'/') cfullpath from my_tree where cfullpath=? "+
                    " union all ";
        }

        sql+="\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent,(case when cfullpath like '/%' then cfullpath else '/'||cfullpath end) cfullpath from my_tree where cparent in (select cid from my_tree where cfullpath = ?)";
        Object[] params = null;
        if (!Constant.PATH_SEP.equals(path)) {
            params = new Object[]{path, path};
        }else{
            params = new Object[]{path};
        }
        return jdbcTemplate.query(sql, params, new BeanPropertyRowMapper(ExtendedFileEntry.class));
//        }else{
//            String sql = "WITH RECURSIVE my_tree AS (\n" +
//                    "\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
//                    "\t\t\tunion\n" +
//                    "\t\t\tselect b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, aaa.cname||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//                    "\t\t\t)\n" +
//                    "\t\t\tselect * from my_tree";
//            return jdbcTemplate.query(sql, new BeanPropertyRowMapper(ExtendedFileEntry.class));
//        }

//        String fileOnlySql = "select * from tfiles where cpath = ?";
//        if (includeSubDir) {
//                //cid, cname, ccrt, csize, cpath
//            return jdbcTemplate.query("select distinct cast(? as int) cid, "+
//                    "cast(left(cpath,strpos(right(cpath,?),?)) as varchar) cname, "+
//                    "cast(null as timestamp) ccrtdt, "+
//                    "cast(0 as bigint) csize, "+
//                    "cast(null as varchar) cpath, cast(0 as int) cisdir from tfiles where cpath <> ? and cpath like ?||?"+
//                    "union all "+
//                    fileOnlySql, new Object[]{-1, 0-path.length(), path, path, path, "%", path}, new BeanPropertyRowMapper(FileEntry.class));
//        } else {
//            return jdbcTemplate.query(fileOnlySql, new Object[]{path}, new BeanPropertyRowMapper(FileEntry.class));
//        }
//        if (includeSubDir){
//
//        }else{
//            return jdbcTemplate.query("select  node.cid, node.cname, node.ccrtdt, node.csize, coalesce(node.cpath,parent.cname) cpath, coalesce(node.cpath,parent.cname)||node.cname cfullpath, node.cisdir, node.cleft, node.cright from tfiles node, tfiles parent where node.cleft between parent.cleft and parent.cright and parent.cname=?", new Object[]{path}, new BeanPropertyRowMapper(FileEntry.class));
//        }
    }

//    @Override
//    public List<FileEntry> getFiles(int parentId, boolean includeSubDir) {
//        if (parentId==0){
//            String sql = "WITH RECURSIVE my_tree AS (\n" +
//                    "\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent, coalesce(cname,'/') cfullpath from tfiles where cparent is null\n" +
//                    "\t\t\tunion\n" +
//                    "\t\t\tselect b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, cfullpath||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//                    "\t\t\t)\n" +
//                    "\t\t\tselect * from my_tree";
//            return jdbcTemplate.query(sql, new BeanPropertyRowMapper(ExtendedFileEntry.class));
//        }else{
//            String sql = "WITH RECURSIVE my_tree AS (\n" +
//                    "\t\t\tselect cid,cname,ccrtdt,csize,cisdir,cparent, cname cfullpath from tfiles where cparent = ?\n" +
//                    "\t\t\tunion\n" +
//                    "\t\t\tselect b.cid,b.cname,b.ccrtdt,b.csize,b.cisdir,b.cparent, cfullpath||'/'||b.cname cfullpath from my_tree aaa, tfiles b where aaa.cid = b.cparent\n" +
//                    "\t\t\t)\n" +
//                    "\t\t\tselect * from my_tree";
//            return jdbcTemplate.query(sql, new Object[]{parentId}, new BeanPropertyRowMapper(ExtendedFileEntry.class));
//        }
//    }

    public FileEntry getByParentAndName(int parentId, String name){
        return (FileEntry) jdbcTemplate.queryForObject("select * from tfiles where cparent = ? and cname = ? limit 1", new Object[]{parentId, name}, new BeanPropertyRowMapper(FileEntry.class));
    }

//    @Override
//    public FileEntry getByName(String path, String name) {
//        return (FileEntry) jdbcTemplate.queryForObject("select * from tfiles where cpath = ? and cname = ?", new Object[]{path, name}, new BeanPropertyRowMapper(FileEntry.class));
//    }
//
//    @Override
//    public FileEntry getById(int fileId) {
//        return (FileEntry) jdbcTemplate.queryForObject("select * from tfiles where cid = ?", new Object[]{fileId}, new BeanPropertyRowMapper(FileEntry.class));
//    }
//
//    @Override
//    public void deleteAllFiles() {
//        jdbcTemplate.update("delete from tfiles");
//    }
//
//    @Override
//    public void deleteByPath(String path) {
//        jdbcTemplate.update("delete from tfiles where cpath = ?", path);
//    }
}
