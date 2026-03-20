package com.example.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ClassloaderReaderSource implements ReaderSource {
    private final ClassLoader classLoader;

    public ClassloaderReaderSource(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Reader get(String path) throws IOException {
        InputStream in = classLoader.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("No such resource " + path);
        }
        return new InputStreamReader(in);
    }
}
