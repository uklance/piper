package com.example.template;

import com.example.expression.Expression;
import com.example.expression.ExpressionParser;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class TemplateParser {

    enum TokenType {
        TEXT, INTERP_START, INTERP_END,
        IF_START, LIST_START, ELSE_TAG, IF_END, LIST_END,
        TAG_END,
        EOF
    }

    enum Mode {
        DIRECTIVE_OPEN, INTERP;
    }

    private static class Token {
        final TokenType type;
        final String text;
        Token(TokenType t, String txt) { type = t; text = txt; }
    }

    private static class Lexer {
        private final String s;
        private int pos;
        private Token next;
        private Deque<Mode> modes = new LinkedList<>();

        Lexer(String s) {
            this.s = s;
            this.next = parseNext();
        }

        public TokenType peekType() {
            return next.type;
        }

        public Mode peekMode() {
            return modes.peek();
        }

        public void popMode(Mode expected) {
            if (peekMode() != expected) {
                throw new RuntimeException("Expected " + expected + " but got " + peekMode());
            }
            modes.pop();
        }

        public void pushMode(Mode mode) {
            modes.push(mode);
        }

        public Token next() {
            Token current = next;
            next = parseNext();
            return current;
        }

        public Token next(TokenType expected) {
            if (peekType() != expected) {
                throw new RuntimeException("Expected " + expected + " but got " + peekType());
            }
            return next();
        }

        private boolean startsWith(String prefix) {
            return s.startsWith(prefix, pos);
        }

        private boolean startsWith(char prefix) {
            return s.charAt(pos) == prefix;
        }

        private Token parseNext() {
            //skipWhitespace();

            if (pos >= s.length()) return new Token(TokenType.EOF, "");
            if (startsWith("</#")) {
                if (startsWith("</#list>")) {
                    pos += 8;
                    return new Token(TokenType.LIST_END, "</#list>");
                }
                if (startsWith("</#if>")) {
                    pos += 6;
                    return new Token(TokenType.IF_END, "</#if>");
                }
                throw new RuntimeException("Unexpected close tag");
            }
            if (startsWith("<#")) {
                if (startsWith("<#else>")) {
                    pos += 7;
                    return new Token(TokenType.ELSE_TAG, "<#else>");
                }
                if (startsWith("<#list")) {
                    pos += 6;
                    pushMode(Mode.DIRECTIVE_OPEN);
                    return new Token(TokenType.LIST_START, "<#list");
                }
                if (startsWith("<#if")) {
                    pos += 4;
                    pushMode(Mode.DIRECTIVE_OPEN);
                    return new Token(TokenType.IF_START, "<#if");
                }
                throw new RuntimeException("Unexpected open tag");
            }
            if (startsWith("${")) {
                pos += 2;
                pushMode(Mode.INTERP);
                return new Token(TokenType.INTERP_START, "${");
            }
            if (startsWith("}")) {
                pos++;
                popMode(Mode.INTERP);
                return new Token(TokenType.INTERP_END, "}");
            }
            if (startsWith('>')) {
                pos++;
                popMode(Mode.DIRECTIVE_OPEN);
                return new Token(TokenType.TAG_END, ">");
            }

            int start = pos;
            if (peekMode() == Mode.INTERP) {
                while (pos < s.length()) {
                    if (startsWith('}')) {
                        break;
                    }
                    pos++;
                }
            } else if (peekMode() == Mode.DIRECTIVE_OPEN) {
                while (pos < s.length()) {
                    if (startsWith('>')) {
                        break;
                    }
                    pos++;
                }
            } else {
                while (pos < s.length()) {
                    if (startsWith("</#") ||
                            startsWith("<#") ||
                            startsWith("${") ||
                            startsWith('}') ||
                            startsWith('>')) {
                        break;
                    }
                    pos++;
                }
            }
            String text = s.substring(start, pos);
            return new Token(TokenType.TEXT, text);
        }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }
    }

    private final ExpressionParser expressionParser;

    public TemplateParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }

    public Template parse(String template) {
        Lexer lexer = new Lexer(template);
        List<Node> nodes = parseNodes(lexer);
        if (lexer.peekType() != TokenType.EOF) {
            throw new RuntimeException("Unexpected token after parsing: " + lexer.peekType());
        }
        return (context, sink) -> {
            for (Node node : nodes) {
                node.render(context, sink);
            }
        };
    }

    private List<Node> parseNodes(Lexer lexer) {
        List<Node> nodes = new ArrayList<>();

        while (lexer.peekType() != TokenType.EOF &&
                lexer.peekType() != TokenType.ELSE_TAG &&
                lexer.peekType() != TokenType.IF_END &&
                lexer.peekType() != TokenType.LIST_END) {

            switch (lexer.peekType()) {
                case TEXT:
                    nodes.add(parseText(lexer));
                    break;

                case INTERP_START:
                    nodes.add(parseInterpolation(lexer));
                    break;

                case IF_START:
                    nodes.add(parseIf(lexer));
                    break;

                case LIST_START:
                    nodes.add(parseList(lexer));
                    break;

                default:
                    throw new RuntimeException("Unexpected NodeType: " + lexer.peekType());
            }
        }
        return nodes;
    }

    private Node parseText(Lexer lexer) {
        Token token = lexer.next(TokenType.TEXT);
        String text = token.text;
        return (context, sink) -> sink.accept(text);
    }

    private Node parseInterpolation(Lexer lexer) {
        lexer.next(TokenType.INTERP_START);

        StringBuilder exprText = new StringBuilder();
        while (lexer.peekType() != TokenType.INTERP_END && lexer.peekType() != TokenType.EOF) {
            exprText.append(lexer.next(TokenType.TEXT).text);
        }
        lexer.next(TokenType.INTERP_END);

        String trimmed = exprText.toString().trim();
        if (trimmed.isEmpty()) {
            throw new RuntimeException("Empty interpolation expression");
        }
        Expression expr = expressionParser.parse(trimmed);
        return (context, sink) -> {
            Object result = expr.eval(context);
            sink.accept(context.convert(result, String.class));
        };
    }

    private Node parseIf(Lexer lexer) {
        lexer.next(TokenType.IF_START);

        StringBuilder condText = new StringBuilder();
        while (lexer.peekType() != TokenType.TAG_END && lexer.peekType() != TokenType.EOF) {
            Token t = lexer.next();
            condText.append(t.text);
        }
        lexer.next(TokenType.TAG_END);

        Expression condExpr = expressionParser.parse(condText.toString().trim());
        List<Node> thenNodes = parseNodes(lexer);
        List<Node> elseNodes;
        if (lexer.peekType() == TokenType.ELSE_TAG) {
            lexer.next(TokenType.ELSE_TAG);
            elseNodes = parseNodes(lexer);
        } else {
            elseNodes = null;
        }
        lexer.next(TokenType.IF_END);
        return (context, sink) -> {
            boolean cond = context.isTruthy(condExpr.eval(context));
            if (cond) {
                for (Node thenNode : thenNodes) {
                    thenNode.render(context, sink);
                }
            } else if (elseNodes != null){
                for (Node elseNode : elseNodes) {
                    elseNode.render(context, sink);
                }
            }
        };
    }

    private Node parseList(Lexer lexer) {
        lexer.next(TokenType.LIST_START);

        StringBuilder headerText = new StringBuilder();
        while (lexer.peekType() != TokenType.TAG_END && lexer.peekType() != TokenType.EOF) {
            headerText.append(lexer.next().text);
        }

        lexer.next(TokenType.TAG_END);

        String header = headerText.toString().trim();
        int asIdx = header.indexOf(" as ");
        if (asIdx < 0) {
            throw new RuntimeException("Invalid list header (missing 'as'): " + header);
        }

        String listExprText = header.substring(0, asIdx).trim();
        String varName = header.substring(asIdx + 4).trim();

        if (listExprText.isEmpty() || varName.isEmpty()) {
            throw new RuntimeException("Invalid list header: " + header);
        }

        Expression listExpr = expressionParser.parse(listExprText);
        List<Node> body = parseNodes(lexer);
        lexer.next(TokenType.LIST_END);

        return (context, sink) -> {
            Iterable list = listExpr.eval(context, Iterable.class);
            for (Object var : list) {
                context.set(varName, var);
                for (Node node : body) {
                    node.render(context, sink);
                }
            }
        };
    }
}