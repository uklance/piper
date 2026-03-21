package com.example.loader;

import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.util.Map;

public class MapTemplateLoader implements TemplateLoader {
    private final Map<String, String> map;

    public MapTemplateLoader(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Template load(String path, TemplateParser parser) throws IOException {
        String template = map.get(path);
        if (template == null) {
            throw new IOException("No such resource " + path);
        }
        return parser.parse(template);
    }
}
