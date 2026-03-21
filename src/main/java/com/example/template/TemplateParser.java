package com.example.template;

import com.example.expression.EvalContext;
import com.example.expression.EvalContextFactory;
import com.example.expression.Expression;
import com.example.expression.ExpressionParser;
import com.example.loader.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateParser {
    private final TemplateLoader templateLoader;
    private final ExpressionParser expressionParser;
    private final EvalContextFactory evalContextFactory;

    public TemplateParser(ExpressionParser expressionParser, TemplateLoader templateLoader, EvalContextFactory evalContextFactory) {
        this.templateLoader = templateLoader;
        this.expressionParser = expressionParser;
        this.evalContextFactory = evalContextFactory;
    }

    public Template parse(Reader reader) throws IOException {
        return parse(new TemplateLexer(reader));
    }

    public Template parse(String template) throws IOException {
        return parse(new TemplateLexer(template));
    }

    private Template parse(TemplateLexer lexer) throws IOException {
        List<Node> nodes = parseNodes(lexer, t -> false);

        if (lexer.peekType() != TokenType.EOF) {
            throw new RuntimeException("Unexpected token after parsing: " + lexer.peekType());
        }

        return new Template() {
            @Override
            public void apply(EvalContext context, StringSink sink) throws Exception {
                for (Node node : nodes) {
                    node.render(context, sink);
                }
            }

            @Override
            public void apply(Map<String, ?> contextMap, StringSink sink) throws Exception {
                EvalContext context = evalContextFactory.create(contextMap);
                for (Node node : nodes) {
                    node.render(context, sink);
                }
            }
        };
    }

    private List<Node> parseNodes(TemplateLexer lexer, Predicate<TemplateToken> stopPredicate) throws IOException {
        List<Node> nodes = new ArrayList<>();

        for (TemplateToken token = lexer.peek(); !(token.type == TokenType.EOF || stopPredicate.test(token)); token = lexer.peek()) {
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
                        case "assign":
                            nodes.add(parseAssign(lexer, args));
                            break;
                        case "include":
                            nodes.add(parseInclude(lexer, args));
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

    private Node parseText(TemplateLexer lexer) {
        String text = lexer.next(TokenType.TEXT).text;
        return (context, sink) -> sink.accept(text);
    }

    private Node parseInterpolation(TemplateLexer lexer) {
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

    private boolean isDirectiveStart(TemplateToken token, String text) {
        if (token.type == TokenType.DIRECTIVE_START && token.text.startsWith(text)) {
            return text.length() == token.text.length() || Character.isWhitespace(token.text.charAt(text.length()));
        }
        return false;
    }

    private boolean isDirectiveEnd(TemplateToken token, String text) {
        return token.type == TokenType.DIRECTIVE_END && text.equals(token.text);
    }

    private static class IfBranch {
        public IfBranch(Expression test, List<Node> nodes) {
            this.test = test;
            this.nodes = nodes;
        }
        private final Expression test;
        private final List<Node> nodes;
    }

    private Node parseIf(TemplateLexer lexer, String args) throws IOException {
        List<IfBranch> branches = new ArrayList<>();
        while (lexer.peekType() == TokenType.DIRECTIVE_START && !isDirectiveStart(lexer.peek(), "else")) {
            TemplateToken token = lexer.next(TokenType.DIRECTIVE_START); // might be <#if ...> or <#elseif ...>
            int spaceIndex = token.text.indexOf(' ');
            if (spaceIndex < 0) {
                throw new RuntimeException("Invalid args: " + token.text);
            }
            Expression expr = expressionParser.parse(token.text.substring(spaceIndex + 1));
            List<Node> nodes = parseNodes(lexer, t ->
                    isDirectiveStart(t, "else") ||
                    isDirectiveStart(t, "elseif") ||
                    isDirectiveEnd(t, "if"));

            branches.add(new IfBranch(expr, nodes));
        }
        List<Node> elseNodes;
        if (lexer.peekType() == TokenType.DIRECTIVE_START) {
            lexer.next(TokenType.DIRECTIVE_START); // consume <#else>
            elseNodes = parseNodes(lexer, t -> isDirectiveEnd(t, "if"));
        } else {
            elseNodes = null;
        }

        lexer.next(TokenType.DIRECTIVE_END); // consume </#if>

        return (context, sink) -> {
            List<Node> chosen = elseNodes;
            for (IfBranch branch : branches) {
                if (context.isTruthy(branch.test.eval(context))) {
                    chosen = branch.nodes;
                    break;
                }
            }
            if (chosen != null) {
                for (Node n : chosen) {
                    n.render(context, sink);
                }
            }
        };
    }

    private static final Pattern LIST_PATTERN = Pattern.compile("\\s*(.+?)\\s* as \\s*(.+?)\\s*");
    private Node parseList(TemplateLexer lexer, String args) throws IOException {
        lexer.next(TokenType.DIRECTIVE_START);
        Matcher matcher = LIST_PATTERN.matcher(args);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid list args: " + args);
        }

        Expression listExpr = expressionParser.parse(matcher.group(1));
        String varName = matcher.group(2);

        List<Node> body = parseNodes(lexer, t -> isDirectiveEnd(t, "list"));
        lexer.next(TokenType.DIRECTIVE_END);

        return (context, sink) -> {
            Iterable<?> list = listExpr.eval(context, Iterable.class);
            for (Object item : list) {
                context.setValue(varName, item);
                for (Node n : body) {
                    n.render(context, sink);
                }
            }
        };
    }

    private static final Pattern ASSIGN_PATTERN = Pattern.compile("\\s*(.+?)\\s*=\\s*(.+?)\\s*");
    private Node parseAssign(TemplateLexer lexer, String args) {
        lexer.next(TokenType.DIRECTIVE_START);
        Matcher matcher = ASSIGN_PATTERN.matcher(args);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid assign args: " + args);
        }
        String varName = matcher.group(1);
        Expression expr = expressionParser.parse(matcher.group(2));
        return (context, sink) -> context.setValue(varName, expr.eval(context));
    }

    private Node parseInclude(TemplateLexer lexer, String args) {
        lexer.next(TokenType.DIRECTIVE_START);
        Expression expr = expressionParser.parse(args);
        return (context, sink) -> {
            String path = expr.eval(context, String.class);
            Template include = templateLoader.load(path, this);
            include.apply(context, sink);
        };
    }
}