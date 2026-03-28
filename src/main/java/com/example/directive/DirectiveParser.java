package com.example.directive;

import com.example.template.TemplateLexer;
import com.example.template.TemplateNode;

import java.io.IOException;

public interface DirectiveParser {
    String getName();
    TemplateNode parse(TemplateLexer lexer, String args, DirectiveParserContext context) throws IOException;
}
