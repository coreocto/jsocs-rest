package org.coreocto.dev.jsocs.rest.pojo;

public class Block {
    private int cid;
    private String cname;
    private int cuse;
    private long csize;
    private String cremoteid;
    private String cowner;
    private String cdirectlink;
    private int caccid;

    public int getCaccid() {
        return caccid;
    }

    public void setCaccid(int caccid) {
        this.caccid = caccid;
    }

    public String getCdirectlink() {
        return cdirectlink;
    }

    public void setCdirectlink(String cdirectlink) {
        this.cdirectlink = cdirectlink;
    }

    public String getCowner() {
        return cowner;
    }

    public void setCowner(String cowner) {
        this.cowner = cowner;
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

    public int getCuse() {
        return cuse;
    }

    public void setCuse(int cuse) {
        this.cuse = cuse;
    }

    public long getCsize() {
        return csize;
    }

    public void setCsize(long csize) {
        this.csize = csize;
    }

    public String getCremoteid() {
        return cremoteid;
    }

    public void setCremoteid(String cremoteid) {
        this.cremoteid = cremoteid;
    }
}
