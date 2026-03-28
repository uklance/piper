package com.example.directive;

import com.example.expression.Expression;
import com.example.template.Node;
import com.example.template.TemplateLexer;
import com.example.template.TemplateToken;
import com.example.template.TokenType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IfDirectiveParser implements DirectiveParser {
    private static class IfBranch {
        public IfBranch(Expression test, List<Node> nodes) {
            this.test = test;
            this.nodes = nodes;
        }
        private final Expression test;
        private final List<Node> nodes;
    }

    @Override
    public String getName() {
        return "if";
    }

    @Override
    public Node parse(TemplateLexer lexer, String args, Context context) throws IOException {
        List<IfBranch> branches = new ArrayList<>();
        while (lexer.peekType() == TokenType.DIRECTIVE_START && !context.isDirectiveStart(lexer.peek(), "else")) {
            TemplateToken token = lexer.next(TokenType.DIRECTIVE_START); // might be <#if ...> or <#elseif ...>
            int spaceIndex = token.text.indexOf(' ');
            if (spaceIndex < 0) {
                throw new RuntimeException("Invalid args: " + token.text);
            }
            Expression expr = context.parseExpression(token.text.substring(spaceIndex + 1));
            List<Node> nodes = context.parseNodes(lexer, t ->
                    context.isDirectiveStart(t, "else") ||
                    context.isDirectiveStart(t, "elseif") ||
                    context.isDirectiveEnd(t, "if"));

            branches.add(new IfBranch(expr, nodes));
        }
        List<Node> elseNodes;
        if (lexer.peekType() == TokenType.DIRECTIVE_START) {
            lexer.next(TokenType.DIRECTIVE_START); // consume <#else>
            elseNodes = context.parseNodes(lexer, t -> context.isDirectiveEnd(t, "if"));
        } else {
            elseNodes = null;
        }

        lexer.next(TokenType.DIRECTIVE_END); // consume </#if>

        return (evalContext, sink) -> {
            List<Node> chosen = elseNodes;
            for (IfBranch branch : branches) {
                if (evalContext.isTruthy(branch.test.eval(evalContext))) {
                    chosen = branch.nodes;
                    break;
                }
            }
            if (chosen != null) {
                for (Node n : chosen) {
                    n.render(evalContext, sink);
                }
            }
        };
    }
}
