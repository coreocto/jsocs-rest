package org.coreocto.dev.jsocs.rest.exception;

import java.nio.file.FileAlreadyExistsException;

public class FolderAlreadyExistsException extends FileAlreadyExistsException {
    public FolderAlreadyExistsException(String file) {
        super(file);
    }
}
