package com.example.directive;

import com.example.expression.Expression;
import com.example.template.Node;
import com.example.template.Template;
import com.example.template.TemplateLexer;
import com.example.template.TokenType;

import java.io.IOException;

public class IncludeDirectiveParser implements DirectiveParser {
    @Override
    public String getName() {
        return "include";
    }

    @Override
    public Node parse(TemplateLexer lexer, String args, Context context) throws IOException {
        lexer.next(TokenType.DIRECTIVE_START);
        Expression expr = context.parseExpression(args);
        return (evalContext, sink) -> {
            String path = expr.eval(evalContext, String.class);
            Template include = context.loadTemplate(path);
            include.apply(evalContext, sink);
        };
    }
}
