package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "trequest")
public class RequestEntry {
    @Id
    @SequenceGenerator(name="identifier", sequenceName="trequest_cid_seq", allocationSize=1)
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="identifier")
    private Integer cid;
    private Date ccrtdt;
    private String crequesturi;
    private String cresponse;
    private Date cupddt;

    public Date getCupddt() {
        return cupddt;
    }

    public void setCupddt(Date cupddt) {
        this.cupddt = cupddt;
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

    public String getCrequesturi() {
        return crequesturi;
    }

    public void setCrequesturi(String crequesturi) {
        this.crequesturi = crequesturi;
    }

    public String getCresponse() {
        return cresponse;
    }

    public void setCresponse(String cresponse) {
        this.cresponse = cresponse;
    }
}
