package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class FileTableId implements Serializable {

    private int cfileid;
    private int cblkid;

    public int getCfileid() {
        return cfileid;
    }

    public void setCfileid(int cfileid) {
        this.cfileid = cfileid;
    }

    public int getCblkid() {
        return cblkid;
    }

    public void setCblkid(int cblkid) {
        this.cblkid = cblkid;
    }
}
