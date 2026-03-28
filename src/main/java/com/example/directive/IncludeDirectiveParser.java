package com.example.directive;

import com.example.expression.Expression;
import com.example.template.TemplateNode;
import com.example.template.Template;
import com.example.template.TemplateLexer;
import com.example.template.TemplateTokenType;

import java.io.IOException;

public class IncludeDirectiveParser implements DirectiveParser {
    @Override
    public String getName() {
        return "include";
    }

    @Override
    public TemplateNode parse(TemplateLexer lexer, String args, DirectiveParserContext context) throws IOException {
        lexer.next(TemplateTokenType.DIRECTIVE_START);
        Expression expr = context.parseExpression(args);
        return (evalContext, sink) -> {
            String path = expr.eval(evalContext, String.class);
            Template include = context.loadTemplate(path);
            include.apply(evalContext, sink);
        };
    }
}
