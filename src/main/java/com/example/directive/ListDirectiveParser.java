package com.example.directive;

import com.example.expression.Expression;
import com.example.template.Node;
import com.example.template.TemplateLexer;
import com.example.template.TokenType;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListDirectiveParser implements DirectiveParser {
    private static final Pattern LIST_PATTERN = Pattern.compile("\\s*(.+?)\\s* as \\s*(.+?)\\s*");

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public Node parse(TemplateLexer lexer, String args, Context context) throws IOException {
        lexer.next(TokenType.DIRECTIVE_START);
        Matcher matcher = LIST_PATTERN.matcher(args);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid list args: " + args);
        }

        Expression listExpr = context.parseExpression(matcher.group(1));
        String varName = matcher.group(2);

        List<Node> body = context.parseNodes(lexer, t -> context.isDirectiveEnd(t, "list"));
        lexer.next(TokenType.DIRECTIVE_END);

        return (evalContext, sink) -> {
            Iterable<?> list = listExpr.eval(evalContext, Iterable.class);
            for (Object item : list) {
                evalContext.setValue(varName, item);
                for (Node n : body) {
                    n.render(evalContext, sink);
                }
            }
        };
    }
}
