package com.example.template;

import java.io.IOException;
import java.io.Reader;

public interface TemplateParser {
    Template parse(Reader reader) throws IOException;
    Template parse(String template) throws IOException;
}