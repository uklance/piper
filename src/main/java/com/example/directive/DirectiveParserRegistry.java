package com.example.directive;

import com.example.template.TemplateNode;
import com.example.template.TemplateLexer;

import java.io.IOException;

public interface DirectiveParserRegistry {
    TemplateNode parse(TemplateLexer lexer, String name, String args, DirectiveParserContext context) throws IOException;
}
