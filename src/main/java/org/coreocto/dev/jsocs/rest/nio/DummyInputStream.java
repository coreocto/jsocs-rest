package org.coreocto.dev.jsocs.rest.nio;

import java.io.IOException;
import java.io.InputStream;

public class DummyInputStream extends InputStream {

    private int avail;
    private int readBytes = 0;

    public DummyInputStream() {
        avail = Integer.MAX_VALUE;
    }

    public DummyInputStream(int avail) {
        this.avail = avail;
    }

    @Override
    public int read() throws IOException {
        if (readBytes == avail) {
            return -1;
        } else {
            readBytes++;
            return 0;
        }
    }

    public int available() throws IOException {
        return avail;
    }
}
