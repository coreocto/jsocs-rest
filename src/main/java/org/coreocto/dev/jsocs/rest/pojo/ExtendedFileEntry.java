package org.coreocto.dev.jsocs.rest.pojo;

import java.util.Date;

public class ExtendedFileEntry {
    private String cfullpath;

    public String getCfullpath() {
        return cfullpath;
    }

    public void setCfullpath(String cfullpath) {
        this.cfullpath = cfullpath;
    }

    private Integer cid;
    private String cname;
    private Date ccrtdt;
    private Long csize;
    private Integer cisdir;
    private Integer cparent;

    public Integer getCparent() {
        return cparent;
    }

    public void setCparent(Integer cparent) {
        this.cparent = cparent;
    }

    public Integer getCisdir() {
        return cisdir;
    }

    public void setCisdir(Integer cisdir) {
        this.cisdir = cisdir;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public Date getCcrtdt() {
        return ccrtdt;
    }

    public void setCcrtdt(Date ccrtdt) {
        this.ccrtdt = ccrtdt;
    }

    public Long getCsize() {
        return csize;
    }

    public void setCsize(Long csize) {
        this.csize = csize;
    }
}
