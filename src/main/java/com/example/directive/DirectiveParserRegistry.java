package com.example.directive;

import com.example.directive.DirectiveParser.Context;
import com.example.template.TemplateNode;
import com.example.template.TemplateLexer;

import java.io.IOException;

public interface DirectiveParserRegistry {
    TemplateNode parse(TemplateLexer lexer, String name, String args, Context context) throws IOException;
}
