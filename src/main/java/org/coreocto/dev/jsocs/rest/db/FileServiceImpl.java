package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void create(String path, String name, long size) {
        jdbcTemplate.update("insert into tfiles(cpath,cname,ccrtdt,csize) values(?,?,CURRENT_TIMESTAMP,?)", path, name, size);
    }

    @Override
    public void deleteByName(String path, String name) {
        jdbcTemplate.update("delete from tfiles where cname = ? and cpath = ?", name, path);
    }

    @Override
    public void deleteById(int id) {
        jdbcTemplate.update("delete from tfiles where cid = ?", id);
    }

    @Override
    public List<FileEntry> getFiles(String path, boolean includeSubDir) {
        String fileOnlySql = "select * from tfiles where cpath = ?";
        if (includeSubDir) {
                //cid, cname, ccrt, csize, cpath
            return jdbcTemplate.query("select distinct cast(? as int) cid, "+
                    "cast(left(cpath,strpos(right(cpath,?),?)) as varchar) cname, "+
                    "cast(null as timestamp) ccrtdt, "+
                    "cast(0 as bigint) csize, "+
                    "cast(null as varchar) cpath from tfiles where cpath <> ? and cpath like ?||?"+
                    "union all "+
                    fileOnlySql, new Object[]{-1, 0-path.length(), path, path, path, "%", path}, new BeanPropertyRowMapper(FileEntry.class));
        } else {
            return jdbcTemplate.query(fileOnlySql, new Object[]{path}, new BeanPropertyRowMapper(FileEntry.class));
        }
    }

    @Override
    public FileEntry getByName(String path, String name) {
        return (FileEntry) jdbcTemplate.queryForObject("select * from tfiles where cpath = ? and cname = ?", new Object[]{path, name}, new BeanPropertyRowMapper(FileEntry.class));
    }

    @Override
    public FileEntry getById(int fileId) {
        return (FileEntry) jdbcTemplate.queryForObject("select * from tfiles where cid = ?", new Object[]{fileId}, new BeanPropertyRowMapper(FileEntry.class));
    }

    @Override
    public void deleteAllFiles() {
        jdbcTemplate.update("delete from tfiles");
    }

    @Override
    public void deleteByPath(String path) {
        jdbcTemplate.update("delete from tfiles where cpath = ?", path);
    }
}
