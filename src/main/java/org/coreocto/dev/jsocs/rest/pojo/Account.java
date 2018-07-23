package org.coreocto.dev.jsocs.rest.pojo;

import java.util.Date;

public class Account {
    private int cid;
    private Date ccrtdt;
    private String cusername;
    private String cpassword;
    private int cinit;
    private String ctoken;
    private String cauthToken;
    private String ctype;

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

    public int getCinit() {
        return cinit;
    }

    public void setCinit(int cinit) {
        this.cinit = cinit;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
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
