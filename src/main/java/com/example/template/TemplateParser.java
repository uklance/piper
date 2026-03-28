package com.example.template;

import com.example.directive.DirectiveParser;
import com.example.directive.DirectiveParserRegistry;
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

public class TemplateParser {
    private final TemplateLoader templateLoader;
    private final ExpressionParser expressionParser;
    private final EvalContextFactory evalContextFactory;
    private final DirectiveParserRegistry directiveParserRegistry;
    private final DirectiveParser.Context directiveParserContext;

    public TemplateParser(ExpressionParser expressionParser, TemplateLoader templateLoader, EvalContextFactory evalContextFactory, DirectiveParserRegistry directiveParserRegistry) {
        this.templateLoader = templateLoader;
        this.expressionParser = expressionParser;
        this.evalContextFactory = evalContextFactory;
        this.directiveParserRegistry = directiveParserRegistry;
        this.directiveParserContext = new DirectiveParser.Context() {
            @Override
            public boolean isDirectiveStart(TemplateToken token, String text) {
                if (token.type == TokenType.DIRECTIVE_START && token.text.startsWith(text)) {
                    return text.length() == token.text.length() || Character.isWhitespace(token.text.charAt(text.length()));
                }
                return false;
            }

            @Override
            public boolean isDirectiveEnd(TemplateToken token, String text) {
                return token.type == TokenType.DIRECTIVE_END && text.equals(token.text);
            }

            @Override
            public Expression parseExpression(String expr) {
                return expressionParser.parse(expr);
            }

            @Override
            public List<Node> parseNodes(TemplateLexer lexer, Predicate<TemplateToken> stopPredicate) throws IOException {
                return TemplateParser.this.parseNodes(lexer, stopPredicate);
            }

            @Override
            public Template loadTemplate(String path) throws IOException {
                return templateLoader.load(path, TemplateParser.this);
            }
        };
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
                    nodes.add(directiveParserRegistry.parse(lexer, name, args, directiveParserContext));
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
}