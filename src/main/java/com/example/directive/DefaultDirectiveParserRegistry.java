package com.example.directive;

import com.example.template.TemplateNode;
import com.example.template.TemplateLexer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultDirectiveParserRegistry implements DirectiveParserRegistry {
    private final Map<String, DirectiveParser> parsers = new LinkedHashMap<>();

    public void register(DirectiveParser parser) {
        parsers.put(parser.getName(), parser);
    }

    @Override
    public TemplateNode parse(TemplateLexer lexer, String name, String args, DirectiveParser.Context context) throws IOException {
        DirectiveParser parser = parsers.get(name);
        if (parser == null) {
            throw new RuntimeException(String.format("No DirectiveParser registered for '%s'", name));
        }
        return parser.parse(lexer, args, context);
    }
}
