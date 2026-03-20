package com.example.expression;

import java.util.ArrayList;
import java.util.List;

public class ExpressionParser {

    public Expression parse(String text) {
        Lexer lexer = new Lexer(text);
        Node node = parseExpression(lexer, 0);
        return new Expression(node);
    }

    private Node parseExpression(Lexer lexer, int precedence) {

        Node left = parsePrefix(lexer);

        while (precedence < precedence(lexer.peek().type)) {
            Token op = lexer.next();
            left = parseInfix(lexer, left, op);
        }

        return left;
    }

    private Node parsePrefix(Lexer lexer) {

        Token t = lexer.next();

        switch (t.type) {

            case NUMBER: {
                Number value;
                if (t.text.indexOf('.') < 0) {
                    value = Integer.parseInt(t.text);
                } else {
                    value = Double.parseDouble(t.text);
                }
                return ctx -> value;
            }

            case STRING:
                return ctx -> t.text;

            case IDENTIFIER:
                switch (t.text) {
                    case "true":
                        return ctx -> Boolean.TRUE;
                    case "false":
                        return ctx -> Boolean.FALSE;
                    case "null":
                        return ctx -> null;
                    default:
                        return ctx -> ctx.get(t.text);
                }

            case LPAREN: {
                Node n = parseExpression(lexer, 0);
                lexer.next(TokenType.RPAREN);
                return n;
            }

            default:
                throw new RuntimeException("Unexpected " + t.type);
        }
    }

    private Node parseInfix(Lexer lexer, Node left, Token op) {

        switch (op.type) {

            case PLUS:
            case MINUS:
            case STAR:
            case SLASH: {

                int p = precedence(op.type);
                Node r = parseExpression(lexer, p);

                return ctx -> {

                    double a = ((Number) left.eval(ctx)).doubleValue();
                    double b = ((Number) r.eval(ctx)).doubleValue();

                    switch (op.type) {
                        case PLUS:
                            return a + b;
                        case MINUS:
                            return a - b;
                        case STAR:
                            return a * b;
                        default:
                            return a / b;
                    }
                };
            }

            case DOT: {

                String name = lexer.next(TokenType.IDENTIFIER).text;

                if (lexer.peek().type == TokenType.LPAREN)
                    return parseMethodCall(lexer, left, name, false);

                return new PropertyNode(left, name, false);
            }

            case SAFE_DOT: {

                String name = lexer.next(TokenType.IDENTIFIER).text;

                if (lexer.peek().type == TokenType.LPAREN)
                    return parseMethodCall(lexer, left, name, true);

                return new PropertyNode(left, name, true);
            }

            case QUESTION: {

                Node t = parseExpression(lexer, 0);
                lexer.next(TokenType.COLON);
                Node f = parseExpression(lexer, 0);

                return ctx -> ctx.isTruthy(left.eval(ctx)) ? t.eval(ctx) : f.eval(ctx);
            }

            case LBRACKET: {

                Node indexNode = parseExpression(lexer, 0);
                lexer.next(TokenType.RBRACKET);

                return ctx -> {
                    Object target = left.eval(ctx);
                    Integer index = ctx.convert(indexNode.eval(ctx), Integer.class);
                    return ctx.get(target, index);
                };
            }

            case PIPE: {

                String name = lexer.next(TokenType.IDENTIFIER).text;

                List<Node> argNodes = new ArrayList<>();

                // check for method-like arguments
                if (lexer.peek().type == TokenType.LPAREN) {
                    lexer.next(); // consume '('

                    if (lexer.peek().type != TokenType.RPAREN) {
                        // first argument
                        argNodes.add(parseExpression(lexer, 0));

                        // remaining arguments separated by commas
                        while (lexer.peek().type == TokenType.COMMA) {
                            lexer.next(); // consume ','
                            argNodes.add(parseExpression(lexer, 0));
                        }
                    }

                    lexer.next(TokenType.RPAREN); // consume ')'
                }

                return ctx -> {
                    // evaluate arguments
                    Object[] args = new Object[argNodes.size()];
                    for (int i = 0; i < argNodes.size(); ++i) {
                        args[i] = argNodes.get(i).eval(ctx);
                    }
                    return ctx.applyMapper(left.eval(ctx), name, args);
                };
            }
        }

        throw new RuntimeException("Unsupported operator " + op.type);
    }

    private Node parseMethodCall(Lexer lexer, Node target, String name, boolean safe) {

        lexer.next(TokenType.LPAREN);

        List<Node> args = new ArrayList<>();

        if (lexer.peek().type != TokenType.RPAREN) {

            args.add(parseExpression(lexer, 0));

            while (lexer.peek().type == TokenType.COMMA) {
                lexer.next();
                args.add(parseExpression(lexer, 0));
            }
        }

        lexer.next(TokenType.RPAREN);

        return ctx -> {

            Object obj = target.eval(ctx);

            if (obj == null) {
                if (safe) return null;
                throw new RuntimeException("Null target");
            }

            Object[] values = new Object[args.size()];

            for (int i = 0; i < args.size(); i++)
                values[i] = args.get(i).eval(ctx);

            try {
                return ctx.invoke(obj, name, values);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private int precedence(TokenType t) {

        switch (t) {
            case PLUS:
            case MINUS:
                return 10;
            case STAR:
            case SLASH:
                return 20;
            case DOT:
            case SAFE_DOT:
            case LPAREN:
            case LBRACKET:
                return 30;
            case PIPE:
                return 5;
            case QUESTION:
                return 3;
        }

        return 0;
    }
}
