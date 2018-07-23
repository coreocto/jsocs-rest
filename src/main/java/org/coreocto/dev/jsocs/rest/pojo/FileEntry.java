package org.coreocto.dev.jsocs.rest.pojo;

import java.util.Date;

public class FileEntry {
    private int cid;
    private String cname;
    private Date ccrtdt;
    private long csize;
    private String cpath;

    public String getCpath() {
        return cpath;
    }

    public void setCpath(String cpath) {
        this.cpath = cpath;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
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

    public long getCsize() {
        return csize;
    }

    public void setCsize(long csize) {
        this.csize = csize;
    }
}
