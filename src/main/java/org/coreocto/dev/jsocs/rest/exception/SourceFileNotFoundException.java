package org.coreocto.dev.jsocs.rest.exception;

import java.io.FileNotFoundException;

public class SourceFileNotFoundException extends FileNotFoundException {
    public SourceFileNotFoundException(String message) {
        super(message);
    }
}
