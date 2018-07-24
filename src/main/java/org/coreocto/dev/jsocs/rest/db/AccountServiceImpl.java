package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SQL_FIELDS = "cusername, cpassword, ccrtdt, cid, COALESCE(cinit,0) as cinit, ctoken, ctype, cauth_token, ccrtoken";

    //provision
    @Override
    public void create(String username, String type) {
        jdbcTemplate.update("insert into taccounts(cusername, ctype) values(?, lower(?))", username, type);
    }

    //read
    @Override
    public List<Account> getAllAccounts() {
        return jdbcTemplate.query("select " + SQL_FIELDS + " from taccounts order by cid", new BeanPropertyRowMapper(Account.class));
    }

    @Override
    public Account getById(int userId) {
        return (Account) jdbcTemplate.queryForObject("select " + SQL_FIELDS + " from taccounts where cid = ?", new Object[]{userId}, new BeanPropertyRowMapper(Account.class));
    }

    @Override
    public Account getByUsername(String username, String type) {
        return (Account) jdbcTemplate.queryForObject("select " + SQL_FIELDS + " from taccounts where cusername = ? and ctype = ? order by cid", new Object[]{username, type}, new BeanPropertyRowMapper(Account.class));
    }

    //update
    @Override
    public void invalidateToken(int userId) {
        jdbcTemplate.update("update taccounts set ctoken = ? where cid = ?", "pcloud-token", userId);
    }

    @Override
    public void updateToken(int userId, String token) {
        jdbcTemplate.update("update taccounts set ctoken = ? where cid = ?", token, userId);
    }

    @Override
    public void replaceToken(String oldToken, String token) {
        jdbcTemplate.update("update taccounts set ctoken = ? where ctoken = ?", token, oldToken);
    }

    @Override
    public void replaceTokens(String oldToken, String token, String authToken) {
        jdbcTemplate.update("update taccounts set ctoken = ?, cauth_token = ? where ctoken = ?", token, authToken, oldToken);
    }

    @Override
    public void updateAuthToken(int userId, String token) {
        jdbcTemplate.update("update taccounts set cauth_token = ? where cid = ?", token, userId);
    }

    @Override
    public void updateInitStatus(int userId, boolean initDone) {
        int newVal = initDone ? 1 : 0;
        jdbcTemplate.update("update taccounts set cinit = ? where cid = ?", newVal, userId);
    }

    //delete
    @Override
    public void deleteById(int userId) {
        jdbcTemplate.update("delete from taccounts where cid = ?", userId);
    }

    @Override
    public void deleteByUsername(String username) {
        jdbcTemplate.update("delete from taccounts where cusername = ?", username);
    }

    @Override
    public void deleteAllAccounts() {
        jdbcTemplate.update("delete from taccounts");
    }

}
