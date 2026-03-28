package com.example.directive;

import com.example.expression.Expression;
import com.example.template.TemplateNode;
import com.example.template.TemplateLexer;
import com.example.template.TemplateTokenType;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssignDirectiveParser implements DirectiveParser {
    private static final Pattern ASSIGN_PATTERN = Pattern.compile("\\s*(.+?)\\s*=\\s*(.+?)\\s*");

    @Override
    public String getName() {
        return "assign";
    }

    @Override
    public TemplateNode parse(TemplateLexer lexer, String args, Context context) throws IOException {
        lexer.next(TemplateTokenType.DIRECTIVE_START);
        Matcher matcher = ASSIGN_PATTERN.matcher(args);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid assign args: " + args);
        }
        String varName = matcher.group(1);
        Expression expr = context.parseExpression(matcher.group(2));
        return (evalContext, sink) -> evalContext.setValue(varName, expr.eval(evalContext));
    }
}
