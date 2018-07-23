package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.FileTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileTableServiceImpl implements FileTableService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void create(int fileId, int blockId) {
        jdbcTemplate.update("insert into tfiletable(cfileid,cblkid) values(?,?)", fileId, blockId);
    }

    @Override
    public void delete(int fileId, int blockId) {
        jdbcTemplate.update("delete from tfiletable where cfileid = ? and cblkid = ?", fileId, blockId);
    }

    @Override
    public void deleteByFileId(int fileId) {
        jdbcTemplate.update("delete from tfiletable where cfileid = ?", fileId);
    }

    @Override
    public List<FileTable> getByFileId(int fileId) {
        return jdbcTemplate.query("select * from tfiletable where cfileid = ?", new Object[]{fileId}, new BeanPropertyRowMapper(FileTable.class));
    }
}
