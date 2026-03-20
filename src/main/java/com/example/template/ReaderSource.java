package com.example.template;

import java.io.IOException;
import java.io.Reader;

public interface ReaderSource {
    Reader get(String path) throws IOException;
}
