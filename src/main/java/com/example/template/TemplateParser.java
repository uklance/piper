package com.example.template;

import com.example.expression.Expression;
import com.example.expression.ExpressionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TemplateParser {

    enum TokenType {
        TEXT,
        INTERPOLATION,
        DIRECTIVE_START,
        DIRECTIVE_END,
        EOF
    }

    private static class Token {
        final TokenType type;
        final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static class Lexer {
        private final String s;
        private int pos;
        private Token next;

        Lexer(String s) {
            this.s = s;
            this.next = parseNext();
        }

        Token peek() {
            return next;
        }

        TokenType peekType() {
            return next.type;
        }

        Token next() {
            Token t = next;
            next = parseNext();
            return t;
        }

        Token next(TokenType expected) {
            if (next.type != expected) {
                throw new RuntimeException("Expected " + expected + " but got " + next.type);
            }
            return next();
        }

        private boolean startsWith(String str) {
            return s.startsWith(str, pos);
        }

        private Token parseNext() {
            if (pos >= s.length()) {
                return new Token(TokenType.EOF, "");
            }

            // -------- INTERPOLATION --------
            if (startsWith("${")) {
                int start = pos + 2;
                int end = s.indexOf('}', start);
                if (end < 0) throw new RuntimeException("Unclosed interpolation");

                String expr = s.substring(start, end).trim();
                pos = end + 1;
                return new Token(TokenType.INTERPOLATION, expr);
            }

            // -------- DIRECTIVE END (</#...>) --------
            if (startsWith("<#")) {
                int start = pos + 2;
                int end = s.indexOf('>', start);
                if (end < 0) throw new RuntimeException("Unclosed directive start");

                String name = s.substring(start, end).trim();
                pos = end + 1;
                return new Token(TokenType.DIRECTIVE_START, name);
            }

            // -------- DIRECTIVE END (</#...>) --------
            if (startsWith("</#")) {
                int start = pos + 3;
                int end = s.indexOf('>', start);
                if (end < 0) throw new RuntimeException("Unclosed directive end");

                String name = s.substring(start, end).trim();
                pos = end + 1;
                return new Token(TokenType.DIRECTIVE_END, name);
            }

            // -------- TEXT --------
            int start = pos;
            while (pos < s.length() && !startsWith("${") && !startsWith("<#") && !startsWith("</#")) {
                pos++;
            }

            return new Token(TokenType.TEXT, s.substring(start, pos));
        }
    }

    private final ExpressionParser expressionParser;

    public TemplateParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }

    public Template parse(String template) {
        Lexer lexer = new Lexer(template);

        List<Node> nodes = parseNodes(lexer, t -> false);

        if (lexer.peekType() != TokenType.EOF) {
            throw new RuntimeException("Unexpected token after parsing: " + lexer.peekType());
        }

        return (context, sink) -> {
            for (Node node : nodes) {
                node.render(context, sink);
            }
        };
    }

    private List<Node> parseNodes(Lexer lexer, Predicate<Token> stopPredicate) {
        List<Node> nodes = new ArrayList<>();

        for (Token token = lexer.peek(); !(token.type == TokenType.EOF || stopPredicate.test(token)); token = lexer.peek()) {
            switch (token.type) {
                case TEXT:
                    nodes.add(parseText(lexer));
                    break;

                case INTERPOLATION:
                    nodes.add(parseInterpolation(lexer));
                    break;

                case DIRECTIVE_START: {
                    int spaceIndex = token.text.indexOf(' ');
                    String name = spaceIndex < 0 ? token.text : token.text.substring(0, spaceIndex);
                    String args = spaceIndex < 0 ? "" : token.text.substring(spaceIndex + 1).trim();

                    switch (name) {
                        case "if":
                            nodes.add(parseIf(lexer, args));
                            break;
                        case "list":
                            nodes.add(parseList(lexer, args));
                            break;
                        default:
                            throw new RuntimeException("Unknown directive: " + name);
                    }
                    break;
                }

                default:
                    throw new RuntimeException(String.format("Unexpected token: %s[%s]", token.type, token.text));
            }
        }

        return nodes;
    }

    private Node parseText(Lexer lexer) {
        String text = lexer.next(TokenType.TEXT).text;
        return (context, sink) -> sink.accept(text);
    }

    private Node parseInterpolation(Lexer lexer) {
        String exprText = lexer.next(TokenType.INTERPOLATION).text;

        if (exprText.isEmpty()) {
            throw new RuntimeException("Empty interpolation expression");
        }

        Expression expr = expressionParser.parse(exprText);

        return (context, sink) -> {
            Object result = expr.eval(context);
            sink.accept(context.convert(result, String.class));
        };
    }

    private boolean isDirectiveStart(Token token, String name) {
        return token.type == TokenType.DIRECTIVE_START && name.equals(token.text);
    }

    private boolean isDirectiveEnd(Token token, String name) {
        return token.type == TokenType.DIRECTIVE_END && name.equals(token.text);
    }

    private Node parseIf(Lexer lexer, String conditionText) {
        lexer.next(TokenType.DIRECTIVE_START);
        Expression condExpr = expressionParser.parse(conditionText);

        List<Node> thenNodes = parseNodes(lexer, t ->
                isDirectiveStart(t, "else") || isDirectiveEnd(t, "if")
        );

        List<Node> elseNodes;
        if (lexer.peekType() == TokenType.DIRECTIVE_START) {
            lexer.next(TokenType.DIRECTIVE_START); // consume <#else>
            elseNodes = parseNodes(lexer, t -> isDirectiveEnd(t, "if"));
        } else {
            elseNodes = null;
        }

        lexer.next(TokenType.DIRECTIVE_END); // consume </#if>

        return (context, sink) -> {
            boolean cond = context.isTruthy(condExpr.eval(context));
            List<Node> chosen = cond ? thenNodes : elseNodes;
            if (chosen != null) {
                for (Node n : chosen) {
                    n.render(context, sink);
                }
            }
        };
    }

    private Node parseList(Lexer lexer, String header) {
        lexer.next(TokenType.DIRECTIVE_START);
        int asIndex = header.indexOf(" as ");
        if (asIndex < 0) {
            throw new RuntimeException("Invalid list header: " + header);
        }

        String listExprText = header.substring(0, asIndex).trim();
        String varName = header.substring(asIndex + 4).trim();

        Expression listExpr = expressionParser.parse(listExprText);

        List<Node> body = parseNodes(lexer, t -> isDirectiveEnd(t, "list"));
        lexer.next(TokenType.DIRECTIVE_END);

        return (context, sink) -> {
            Iterable<?> list = listExpr.eval(context, Iterable.class);
            for (Object item : list) {
                context.set(varName, item);
                for (Node n : body) {
                    n.render(context, sink);
                }
            }
        };
    }
}