package org.coreocto.dev.jsocs.rest.pojo;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "tfiletable")
public class FileTable {
    @EmbeddedId
    private FileTableId id;

    public FileTableId getId() {
        return id;
    }

    public void setId(FileTableId id) {
        this.id = id;
    }
}
