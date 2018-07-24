package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockServiceImpl implements BlockService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void create(String name, long size, int owner) {
        jdbcTemplate.update("insert into tblock(cname,cuse,csize,cremoteid,caccid) values(?,?,?,NULL,?)", name, 1, size, owner);
    }

    @Override
    public void create(String name, long size, int owner, String remoteId) {
        jdbcTemplate.update("insert into tblock(cname,cuse,csize,cremoteid,caccid) values(?,?,?,?,?)", name, 1, size, remoteId, owner);
    }

    @Override
    public void create(String name, long size, int owner, String remoteId, String directLink) {
        jdbcTemplate.update("insert into tblock(cname,cuse,csize,cremoteid,caccid,cdirectLink) values(?,?,?,?,?,?)", name, 1, size, remoteId, owner, directLink);
    }

    @Override
    public List<Block> getBlocks(boolean includeUsed, boolean includeUnused) {
        if (includeUnused && includeUsed) {
            return jdbcTemplate.query("select * from tblock order by cid", new BeanPropertyRowMapper(Block.class));
        } else if (includeUnused) {
            return jdbcTemplate.query("select * from tblock where cuse = ? order by cid", new Object[]{0}, new BeanPropertyRowMapper(Block.class));
        } else if (includeUsed) {
            return jdbcTemplate.query("select * from tblock where cuse = ? order by cid", new Object[]{1}, new BeanPropertyRowMapper(Block.class));
        } else {
            return null;
        }
    }

    @Override
    public List<Block> getByFileId(int fileId) {
        return jdbcTemplate.query("select * from tblock c where\n" +
                "exists(select 1 from tfiletable a, tfiles b where a.cfileid=b.cid and c.cid=a.cblkid and b.cid = ?)\n" +
                "and cuse = ? order by cid asc", new Object[]{fileId, 1}, new BeanPropertyRowMapper(Block.class));
    }

    @Override
    public Block getById(int blockId) {
        return (Block) jdbcTemplate.queryForObject("select * from tblock where cid = ?", new Object[]{blockId}, new BeanPropertyRowMapper(Block.class));
    }

    @Override
    public Block getByName(String name) {
        return (Block) jdbcTemplate.queryForObject("select * from tblock where cname = ?", new Object[]{name}, new BeanPropertyRowMapper(Block.class));
    }

    @Override
    public void update(int blockId, String remoteId, String directLink) {
        jdbcTemplate.update("update tblock set cremoteid = ?, cdirectlink = ? where cid = ?", remoteId, directLink, blockId);
    }

    @Override
    public void update(int blockId, boolean inuse) {
        int newVal = inuse ? 1 : 0;
        jdbcTemplate.update("update tblock set cuse = ? where cid = ?", newVal, blockId);
    }

    @Override
    public void deleteById(int blockId) {
        jdbcTemplate.update("delete from tblock where cid = ?", blockId);
    }

    @Override
    public void deleteAllBlocks() {
        jdbcTemplate.update("delete from tblock");
    }
}
