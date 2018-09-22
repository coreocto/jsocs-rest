package org.coreocto.dev.jsocs.rest.msgraph;

import com.cloudrail.si.interfaces.CloudStorage;

public interface CloudStorageExtended extends CloudStorage {
    public String getHash(String s);
}
