package org.coreocto.dev.jsocs.rest.db;

import org.coreocto.dev.jsocs.rest.pojo.Account;

import java.util.List;

public interface AccountService {

    //provision
    void create(String username, String type);

    //read
    List<Account> getAllAccounts();
    Account getById(int userId);
    Account getByUsername(String username, String type);

    //update
    void invalidateToken(int userId);
    void updateToken(int userId, String token);
    void replaceToken(String oldToken, String token);
    void replaceTokens(String oldToken, String token, String authToken);
    void updateAuthToken(int userId, String token);
    void updateInitStatus(int userId, boolean initDone);

    //delete
    void deleteById(int userId);
    void deleteByUsername(String username);
    void deleteAllAccounts();
}
