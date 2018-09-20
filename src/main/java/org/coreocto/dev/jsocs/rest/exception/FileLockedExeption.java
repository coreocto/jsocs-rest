package org.coreocto.dev.jsocs.rest.exception;

import java.io.IOException;

public class FileLockedExeption extends IOException {
    public FileLockedExeption(String cfullpath) {
        super(cfullpath);
    }
}
