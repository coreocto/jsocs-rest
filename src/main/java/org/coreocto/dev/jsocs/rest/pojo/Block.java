package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tblock")
public class Block {

    @Id
    @SequenceGenerator(name="identifier", sequenceName="tblock_cid_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="identifier")
    private Integer cid;
    private String cname;
    private Integer cuse;
    private long csize;
    private String cremoteid;
    private String cowner;
    private String cdirectlink;
    private Integer caccid;
    private Date ccrtdt;
    private String civ;
    private String chash;

    public String getChash() {
        return chash;
    }

    public void setChash(String chash) {
        this.chash = chash;
    }

    public String getCiv() {
        return civ;
    }

    public void setCiv(String civ) {
        this.civ = civ;
    }

    public Date getCcrtdt() {
        return ccrtdt;
    }

    public void setCcrtdt(Date ccrtdt) {
        this.ccrtdt = ccrtdt;
    }

    public Integer getCaccid() {
        return caccid;
    }

    public void setCaccid(Integer caccid) {
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

    public Integer getCuse() {
        return cuse;
    }

    public void setCuse(Integer cuse) {
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
