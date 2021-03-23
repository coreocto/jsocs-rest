package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "tfiletable")
public class FileTable {
    @EmbeddedId
    private FileTableId id;

    public Date getCcrtdt() {
        return ccrtdt;
    }

    public void setCcrtdt(Date ccrtdt) {
        this.ccrtdt = ccrtdt;
    }

    private Date ccrtdt;

    public FileTableId getId() {
        return id;
    }

    public void setId(FileTableId id) {
        this.id = id;
    }
}
