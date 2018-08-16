package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "taccounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cid;
    private Date ccrtdt;
    private String cusername;
    private String cpassword;
    private Integer cinit;
    private String ctoken;
    private String cauthToken;
    private String ctype;
    private String ccrtoken;

    public Integer getCactive() {
        return cactive;
    }

    public void setCactive(Integer cactive) {
        this.cactive = cactive;
    }

    private Integer cactive;

    public String getCcrtoken() {
        return ccrtoken;
    }

    public void setCcrtoken(String ccrtoken) {
        this.ccrtoken = ccrtoken;
    }

    public String getCtype() {
        return ctype;
    }

    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    public String getCtoken() {
        return ctoken;
    }

    public void setCtoken(String ctoken) {
        this.ctoken = ctoken;
    }

    public String getCauthToken() {
        return cauthToken;
    }

    public void setCauthToken(String cauthToken) {
        this.cauthToken = cauthToken;
    }

    public Integer getCinit() {
        return cinit;
    }

    public void setCinit(Integer cinit) {
        this.cinit = cinit;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public Date getCcrtdt() {
        return ccrtdt;
    }

    public void setCcrtdt(Date ccrtdt) {
        this.ccrtdt = ccrtdt;
    }

    public String getCusername() {
        return cusername;
    }

    public void setCusername(String cusername) {
        this.cusername = cusername;
    }

    public String getCpassword() {
        return cpassword;
    }

    public void setCpassword(String cpassword) {
        this.cpassword = cpassword;
    }

}
