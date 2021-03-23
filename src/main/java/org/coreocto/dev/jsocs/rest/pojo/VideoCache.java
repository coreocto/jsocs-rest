package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tvideocache")
public class VideoCache {

    @Id
    @SequenceGenerator(name = "identifier", sequenceName = "tvideocache_cid_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identifier")
    private Integer cid;
    private Integer cfileid;
    private Date ccrtdt;
    private String cstatus;
    private String cfilename;

    public Date getCcrtdt() {
        return ccrtdt;
    }

    public void setCcrtdt(Date ccrtdt) {
        this.ccrtdt = ccrtdt;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public String getCstatus() {
        return cstatus;
    }

    public void setCstatus(String cstatus) {
        this.cstatus = cstatus;
    }

    public Integer getCfileid() {
        return cfileid;
    }

    public void setCfileid(Integer cfileid) {
        this.cfileid = cfileid;
    }

    public String getCfilename() {
        return cfilename;
    }

    public void setCfilename(String cfilename) {
        this.cfilename = cfilename;
    }
}
