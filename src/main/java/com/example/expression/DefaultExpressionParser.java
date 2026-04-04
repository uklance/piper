package com.example.expression;

import com.example.operation.BinaryOperations;

import java.util.*;

public class DefaultExpressionParser implements ExpressionParser {
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

            case LBRACKET: {
                List<ExpressionNode> elements = new ArrayList<>();

                if (lexer.peek().type != ExpressionTokenType.RBRACKET) {
                    elements.add(parseExpression(lexer, 0));

                    while (lexer.peek().type == ExpressionTokenType.COMMA) {
                        lexer.next();
                        elements.add(parseExpression(lexer, 0));
                    }
                }

                lexer.next(ExpressionTokenType.RBRACKET);

                return ctx -> {
                    List<Object> list = new ArrayList<>(elements.size());
                    for (ExpressionNode e : elements) {
                        list.add(e.eval(ctx));
                    }
                    return list;
                };
            }

            case LBRACE: {
                List<String> keys = new ArrayList<>();
                List<ExpressionNode> valueNodes = new ArrayList<>();

                if (lexer.peek().type != ExpressionTokenType.RBRACE) {

                    do {
                        ExpressionToken keyToken = lexer.next();

                        if (keyToken.type != ExpressionTokenType.STRING &&
                                keyToken.type != ExpressionTokenType.IDENTIFIER) {
                            throw new RuntimeException("Invalid map key");
                        }

                        String key = keyToken.text;

                        lexer.next(ExpressionTokenType.COLON);

                        ExpressionNode value = parseExpression(lexer, 0);

                        keys.add(key);
                        valueNodes.add(value);

                        if (lexer.peek().type != ExpressionTokenType.COMMA)
                            break;

                        lexer.next();

                    } while (true);
                }

                lexer.next(ExpressionTokenType.RBRACE);

                return ctx -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < keys.size(); i++) {
                        map.put(keys.get(i), valueNodes.get(i).eval(ctx));
                    }
                    return map;
                };
            }
            default:
                throw new RuntimeException("Unexpected " + t.type);
        }
    }
    private interface BinaryFunction {
        Object apply(BinaryOperations ops, Object left, Object right);
    }
    private final Map<ExpressionTokenType, BinaryFunction> BINARY_FUNCTIONS = Map.ofEntries(
        Map.entry(ExpressionTokenType.PLUS, BinaryOperations::plus),
        Map.entry(ExpressionTokenType.MINUS, BinaryOperations::minus),
        Map.entry(ExpressionTokenType.STAR, BinaryOperations::multiply),
        Map.entry(ExpressionTokenType.SLASH, BinaryOperations::divide),
        Map.entry(ExpressionTokenType.MOD, BinaryOperations::mod),
        Map.entry(ExpressionTokenType.EQ, (ops, l, r) -> ops.equals(l, r)),
        Map.entry(ExpressionTokenType.NE, (ops, l, r) -> !ops.equals(l, r)),
        Map.entry(ExpressionTokenType.LT, (ops, l, r) -> ops.compare(l, r) < 0),
        Map.entry(ExpressionTokenType.GT, (ops, l, r) -> ops.compare(l, r) > 0),
        Map.entry(ExpressionTokenType.LE, (ops, l, r) -> ops.compare(l, r) <= 0),
        Map.entry(ExpressionTokenType.GE, (ops, l, r) -> ops.compare(l, r) >= 0)
    );

    private ExpressionNode parseInfix(ExpressionLexer lexer, ExpressionNode leftNode, ExpressionToken op) {
        BinaryFunction binaryFunction = BINARY_FUNCTIONS.get(op.type);
        if (binaryFunction != null) {
            ExpressionNode rightNode = parseExpression(lexer, precedence(op.type));
            return ctx -> {
                Object left = leftNode.eval(ctx);
                BinaryOperations binaryOps = ctx.getBinaryOperations(left.getClass());
                Object right = ctx.convert(rightNode.eval(ctx), binaryOps.getType());
                return binaryFunction.apply(binaryOps, left, right);
            };
        }

        switch (op.type) {

            case DOT: {
                String name = lexer.next(ExpressionTokenType.IDENTIFIER).text;
                if (lexer.peek().type == ExpressionTokenType.LPAREN) {
                    return parseMethodCall(lexer, leftNode, name, false);
                }
                return new PropertyExpressionNode(leftNode, name, false);
            }

            case SAFE_DOT: {
                String name = lexer.next(ExpressionTokenType.IDENTIFIER).text;
                if (lexer.peek().type == ExpressionTokenType.LPAREN) {
                    return parseMethodCall(lexer, leftNode, name, true);
                }
                return new PropertyExpressionNode(leftNode, name, true);
            }

            case QUESTION: {
                ExpressionNode t = parseExpression(lexer, 0);
                lexer.next(ExpressionTokenType.COLON);
                ExpressionNode f = parseExpression(lexer, 0);
                return ctx -> ctx.isTruthy(leftNode.eval(ctx)) ? t.eval(ctx) : f.eval(ctx);
            }

            case LBRACKET: {
                ExpressionNode indexNode = parseExpression(lexer, 0);
                lexer.next(ExpressionTokenType.RBRACKET);
                return ctx -> {
                    Object target = leftNode.eval(ctx);
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
                    return ctx.applyMapper(leftNode.eval(ctx), name, args);
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
            for (int i = 0; i < args.size(); i++) {
                values[i] = args.get(i).eval(ctx);
            }

            return ctx.invoke(obj, name, values);
        };
    }

    private int precedence(ExpressionTokenType t) {

        switch (t) {
            case PLUS:
            case MINUS:
                return 10;
            case STAR:
            case SLASH:
            case MOD:
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
            case EQ:
            case NE:
            case LT:
            case GT:
            case LE:
            case GE:
                return 7;
        }

        return 0;
    }
}
