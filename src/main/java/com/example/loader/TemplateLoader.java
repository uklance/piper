package com.example.loader;

import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;

public interface TemplateLoader {
    Template load(String path, TemplateParser parser) throws IOException;
}
