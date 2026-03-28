package com.example.directive;

import com.example.expression.Expression;
import com.example.template.TemplateNode;
import com.example.template.TemplateLexer;
import com.example.template.TemplateToken;
import com.example.template.TemplateTokenType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IfDirectiveParser implements DirectiveParser {
    private static class IfBranch {
        public IfBranch(Expression test, List<TemplateNode> nodes) {
            this.test = test;
            this.nodes = nodes;
        }
        private final Expression test;
        private final List<TemplateNode> nodes;
    }

    @Override
    public String getName() {
        return "if";
    }

    @Override
    public TemplateNode parse(TemplateLexer lexer, String args, DirectiveParserContext context) throws IOException {
        List<IfBranch> branches = new ArrayList<>();
        while (lexer.peekType() == TemplateTokenType.DIRECTIVE_START && !context.isDirectiveStart(lexer.peek(), "else")) {
            TemplateToken token = lexer.next(TemplateTokenType.DIRECTIVE_START); // might be <#if ...> or <#elseif ...>
            int spaceIndex = token.text.indexOf(' ');
            if (spaceIndex < 0) {
                throw new RuntimeException("Invalid args: " + token.text);
            }
            Expression expr = context.parseExpression(token.text.substring(spaceIndex + 1));
            List<TemplateNode> nodes = context.parseNodes(lexer, t ->
                    context.isDirectiveStart(t, "else") ||
                    context.isDirectiveStart(t, "elseif") ||
                    context.isDirectiveEnd(t, "if"));

            branches.add(new IfBranch(expr, nodes));
        }
        List<TemplateNode> elseNodes;
        if (lexer.peekType() == TemplateTokenType.DIRECTIVE_START) {
            lexer.next(TemplateTokenType.DIRECTIVE_START); // consume <#else>
            elseNodes = context.parseNodes(lexer, t -> context.isDirectiveEnd(t, "if"));
        } else {
            elseNodes = null;
        }

        lexer.next(TemplateTokenType.DIRECTIVE_END); // consume </#if>

        return (evalContext, sink) -> {
            List<TemplateNode> chosen = elseNodes;
            for (IfBranch branch : branches) {
                if (evalContext.isTruthy(branch.test.eval(evalContext))) {
                    chosen = branch.nodes;
                    break;
                }
            }
            if (chosen != null) {
                for (TemplateNode n : chosen) {
                    n.render(evalContext, sink);
                }
            }
        };
    }
}
