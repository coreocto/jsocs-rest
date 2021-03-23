package org.coreocto.dev.jsocs.rest.resp;

import com.google.gson.JsonObject;
import org.coreocto.dev.jsocs.rest.exception.*;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

@Service
public class JsonResponseFactory {
    public JsonObject getSuccess() {
        JsonObject newJson = new JsonObject();
        newJson.addProperty("status", "00");
        return newJson;
    }

    public JsonObject getError(Exception ex) {
        JsonObject newJson = new JsonObject();
        if (ex instanceof FolderNotFoundException) {
            newJson.addProperty("status", "50");
            newJson.addProperty("message", "Folder not found: " + ex.getMessage());
        } else if (ex instanceof FileAlreadyExistsException) {
            newJson.addProperty("status", "51");
            newJson.addProperty("message", "File already exists: " + ex.getMessage());
        } else if (ex instanceof SourceFileNotFoundException) {
            newJson.addProperty("status", "52");
            newJson.addProperty("message", "Source file not found: " + ex.getMessage());
        } else if (ex instanceof InsufficientSpaceException) {
            newJson.addProperty("status", "53");
            newJson.addProperty("message", "Insufficient storage space available");
        } else if (ex instanceof CannotWriteTempFileException) {
            newJson.addProperty("status", "54");
            newJson.addProperty("message", "Cannot write to temporary file");
        } else if (ex instanceof InvalidCryptoParamException) {
            newJson.addProperty("status", "55");
            newJson.addProperty("message", "Invalid cryptographic parameter");
        } else if (ex instanceof FileUploadException) {
            newJson.addProperty("status", "56");
            newJson.addProperty("message", "Error occurred when uploading data to cloud storage");
        } else if (ex instanceof FileNotFoundException) {
            newJson.addProperty("status", "57");
            newJson.addProperty("message", "File not found: " + ex.getMessage());
        } else if (ex instanceof FolderAlreadyExistsException) {
            newJson.addProperty("status", "58");
            newJson.addProperty("message", "Folder already exists: " + ex.getMessage());
        } else if (ex instanceof FileNotFoundException) {
            newJson.addProperty("status", "59");
            newJson.addProperty("message", "File not found: " + ex.getMessage());
        } else if (ex instanceof FileLockedExeption) {
            newJson.addProperty("status", "61");
            newJson.addProperty("message", "File: " + ex.getMessage() + " is currently locked, please try again later");
        } else if (ex instanceof IOException) {
            newJson.addProperty("status", "60");
            newJson.addProperty("message","Generic IO error: "+ex.getMessage());
        } else {
            newJson.addProperty("status", "99");
            newJson.addProperty("message","Generic error: "+ex.getMessage());
        }
//        newJson.addProperty("message", ex.getClass().toString() + "<br/>" + ex.getMessage());
        return newJson;
    }
}
