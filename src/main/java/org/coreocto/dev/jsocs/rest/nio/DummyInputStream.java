package org.coreocto.dev.jsocs.rest.nio;

import java.io.IOException;
import java.io.InputStream;

public class DummyInputStream extends InputStream {
    @Override
    public int read() throws IOException {
        return 0;
    }
}
