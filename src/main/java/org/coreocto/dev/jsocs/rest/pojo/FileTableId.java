package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FileTableId implements Serializable {

    private int cfileid;
    private int cblkid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTableId that = (FileTableId) o;
        return cfileid == that.cfileid &&
                cblkid == that.cblkid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cfileid, cblkid);
    }

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
