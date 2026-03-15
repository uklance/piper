package com.example.template;

import java.io.IOException;

public interface StringSink {
    void accept(String s) throws IOException;
}
