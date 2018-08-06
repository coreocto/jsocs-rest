package org.coreocto.dev.jsocs.rest.exception;

import java.io.IOException;

public class CannotWriteTempFileException extends IOException {
    public CannotWriteTempFileException(String path) {
        super(path);
    }
}
