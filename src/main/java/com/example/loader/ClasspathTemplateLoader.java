package com.example.loader;

import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ClasspathTemplateLoader implements TemplateLoader {
    private final ClassLoader classLoader;

    public ClasspathTemplateLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Template load(String path, TemplateParser parser) throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("No such resource " + path);
            }
            return parser.parse(new InputStreamReader(in));
        }
    }
}
