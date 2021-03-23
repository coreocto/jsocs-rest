package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tfiles")
public class FileEntry {

    @Id
    @SequenceGenerator(name="identifier", sequenceName="tfiles_cid_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="identifier")
    private Integer cid;
    private String cname;
    private Date ccrtdt;
    private Long csize;
    private Integer cisdir;
    private Integer cparent;
    private Date clastlock;

    public Date getClastlock() {
        return clastlock;
    }

    public void setClastlock(Date clastlock) {
        this.clastlock = clastlock;
    }

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
