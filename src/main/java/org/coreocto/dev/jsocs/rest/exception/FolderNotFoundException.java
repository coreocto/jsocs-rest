package org.coreocto.dev.jsocs.rest.exception;

import java.io.IOException;

public class FolderNotFoundException extends IOException {
    public FolderNotFoundException(String message) {
        super(message);
    }
}
