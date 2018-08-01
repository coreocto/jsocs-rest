package org.coreocto.dev.jsocs.rest.repo;

import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface AccountRepo extends JpaRepository<Account, Integer> {
    @Query("select f from Account f where f.cusername = ?1 and f.ctype = lower(?2)")
    Account findByNameAndType(String name, String type);

    @Transactional
    @Modifying
    @Query("update Account set ctoken = ?1, cauthToken = ?2 where ctoken = ?3")
    void updateAccessTokenAndCodeByToken(String token, String authToken, String oldToken);

    @Transactional
    @Modifying
    @Query("update Account set ccrtoken = ?2 where cid = ?1")
    void updateCloudRailToken(Integer accId, String token);

//    @Transactional
//    @Modifying
//    Account save(Account account);
//
//    @Transactional
//    @Modifying
//    Account saveAndFlush(Account account);
}
