package com.example.directive;

import com.example.directive.DirectiveParser.Context;
import com.example.template.Node;
import com.example.template.TemplateLexer;

import java.io.IOException;

public interface DirectiveParserRegistry {
    Node parse(TemplateLexer lexer, String name, String args, Context context) throws IOException;
}
