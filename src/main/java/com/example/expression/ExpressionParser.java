package com.example.expression;

import com.example.operation.BinaryOperations;

import java.util.ArrayList;
import java.util.List;

public class ExpressionParser {

    public Expression parse(String text) {
        ExpressionLexer lexer = new ExpressionLexer(text);
        ExpressionNode node = parseExpression(lexer, 0);
        return new Expression(node);
    }

    private ExpressionNode parseExpression(ExpressionLexer lexer, int precedence) {

        ExpressionNode left = parsePrefix(lexer);

        while (precedence < precedence(lexer.peek().type)) {
            ExpressionToken op = lexer.next();
            left = parseInfix(lexer, left, op);
        }

        return left;
    }

    private ExpressionNode parsePrefix(ExpressionLexer lexer) {

        ExpressionToken t = lexer.next();

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
                        return ctx -> ctx.getValue(t.text);
                }

            case LPAREN: {
                ExpressionNode n = parseExpression(lexer, 0);
                lexer.next(ExpressionTokenType.RPAREN);
                return n;
            }

            default:
                throw new RuntimeException("Unexpected " + t.type);
        }
    }

    private ExpressionNode parseInfix(ExpressionLexer lexer, ExpressionNode left, ExpressionToken op) {

        switch (op.type) {
            case PLUS:
            case MINUS:
            case STAR:
            case SLASH: {
                ExpressionNode right = parseExpression(lexer, precedence(op.type));
                return ctx -> {
                    Object leftValue = left.eval(ctx);
                    BinaryOperations binaryOps = ctx.getBinaryOperations(leftValue.getClass());
                    Object rightValue = ctx.convert(right.eval(ctx), binaryOps.getType());

                    switch (op.type) {
                        case PLUS:
                            return binaryOps.plus(leftValue, rightValue);
                        case MINUS:
                            return binaryOps.minus(leftValue, rightValue);
                        case STAR:
                            return binaryOps.multiply(leftValue, rightValue);
                        default:
                            return binaryOps.divide(leftValue, rightValue);
                    }
                };
            }

            case DOT: {

                String name = lexer.next(ExpressionTokenType.IDENTIFIER).text;

                if (lexer.peek().type == ExpressionTokenType.LPAREN)
                    return parseMethodCall(lexer, left, name, false);

                return new PropertyExpressionNode(left, name, false);
            }

            case SAFE_DOT: {

                String name = lexer.next(ExpressionTokenType.IDENTIFIER).text;

                if (lexer.peek().type == ExpressionTokenType.LPAREN)
                    return parseMethodCall(lexer, left, name, true);

                return new PropertyExpressionNode(left, name, true);
            }

            case QUESTION: {

                ExpressionNode t = parseExpression(lexer, 0);
                lexer.next(ExpressionTokenType.COLON);
                ExpressionNode f = parseExpression(lexer, 0);

                return ctx -> ctx.isTruthy(left.eval(ctx)) ? t.eval(ctx) : f.eval(ctx);
            }

            case LBRACKET: {

                ExpressionNode indexNode = parseExpression(lexer, 0);
                lexer.next(ExpressionTokenType.RBRACKET);

                return ctx -> {
                    Object target = left.eval(ctx);
                    Integer index = ctx.convert(indexNode.eval(ctx), Integer.class);
                    return ctx.get(target, index);
                };
            }

            case PIPE: {

                String name = lexer.next(ExpressionTokenType.IDENTIFIER).text;

                List<ExpressionNode> argNodes = new ArrayList<>();

                // check for method-like arguments
                if (lexer.peek().type == ExpressionTokenType.LPAREN) {
                    lexer.next(); // consume '('

                    if (lexer.peek().type != ExpressionTokenType.RPAREN) {
                        // first argument
                        argNodes.add(parseExpression(lexer, 0));

                        // remaining arguments separated by commas
                        while (lexer.peek().type == ExpressionTokenType.COMMA) {
                            lexer.next(); // consume ','
                            argNodes.add(parseExpression(lexer, 0));
                        }
                    }

                    lexer.next(ExpressionTokenType.RPAREN); // consume ')'
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

    private ExpressionNode parseMethodCall(ExpressionLexer lexer, ExpressionNode target, String name, boolean safe) {

        lexer.next(ExpressionTokenType.LPAREN);

        List<ExpressionNode> args = new ArrayList<>();

        if (lexer.peek().type != ExpressionTokenType.RPAREN) {

            args.add(parseExpression(lexer, 0));

            while (lexer.peek().type == ExpressionTokenType.COMMA) {
                lexer.next();
                args.add(parseExpression(lexer, 0));
            }
        }

        lexer.next(ExpressionTokenType.RPAREN);

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

    private int precedence(ExpressionTokenType t) {

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
