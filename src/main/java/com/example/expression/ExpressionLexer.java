package com.example.expression;

import java.util.HashMap;
import java.util.Map;

public class ExpressionLexer {

    private final String input;
    private int pos;

    private static final Map<String, ExpressionTokenType> SYMBOLS = new HashMap<>();

    static {

        SYMBOLS.put("+", ExpressionTokenType.PLUS);
        SYMBOLS.put("-", ExpressionTokenType.MINUS);
        SYMBOLS.put("*", ExpressionTokenType.STAR);
        SYMBOLS.put("/", ExpressionTokenType.SLASH);
        SYMBOLS.put("(", ExpressionTokenType.LPAREN);
        SYMBOLS.put(")", ExpressionTokenType.RPAREN);
        SYMBOLS.put(".", ExpressionTokenType.DOT);
        SYMBOLS.put(",", ExpressionTokenType.COMMA);
        SYMBOLS.put("?", ExpressionTokenType.QUESTION);
        SYMBOLS.put(":", ExpressionTokenType.COLON);
        SYMBOLS.put("|", ExpressionTokenType.PIPE);
        SYMBOLS.put("[", ExpressionTokenType.LBRACKET);
        SYMBOLS.put("]", ExpressionTokenType.RBRACKET);
        SYMBOLS.put("&&", ExpressionTokenType.AND);
        SYMBOLS.put("||", ExpressionTokenType.OR);
        SYMBOLS.put("==", ExpressionTokenType.EQ);
        SYMBOLS.put("!=", ExpressionTokenType.NE);
        SYMBOLS.put("<", ExpressionTokenType.LT);
        SYMBOLS.put(">", ExpressionTokenType.GT);
        SYMBOLS.put("<=", ExpressionTokenType.LE);
        SYMBOLS.put(">=", ExpressionTokenType.GE);
        SYMBOLS.put("?.", ExpressionTokenType.SAFE_DOT);
    }

    public ExpressionLexer(String input) {
        this.input = input;
    }

    public ExpressionToken peek() {
        int save = pos;
        ExpressionToken t = next();
        pos = save;
        return t;
    }

    public ExpressionToken next() {

        skip();

        if (pos >= input.length())
            return new ExpressionToken(ExpressionTokenType.EOF, "");

        char c = input.charAt(pos);

        if (Character.isDigit(c))
            return number();

        if (c == '\'')
            return string();

        if (Character.isLetter(c) || c == '_')
            return identifier();

        return symbol();
    }

    public ExpressionToken next(ExpressionTokenType t) {

        ExpressionToken n = next();

        if (n.type != t)
            throw new RuntimeException("Expected " + t + " got " + n.type);

        return n;
    }

    private void skip() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
            pos++;
    }

    private ExpressionToken number() {

        int start = pos;

        while (pos < input.length() &&
                (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
            pos++;

        return new ExpressionToken(ExpressionTokenType.NUMBER, input.substring(start, pos));
    }

    private ExpressionToken string() {

        pos++;

        int start = pos;

        while (input.charAt(pos) != '\'')
            pos++;

        String s = input.substring(start, pos);

        pos++;

        return new ExpressionToken(ExpressionTokenType.STRING, s);
    }

    private ExpressionToken identifier() {

        int start = pos;

        while (pos < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
            pos++;

        return new ExpressionToken(ExpressionTokenType.IDENTIFIER, input.substring(start, pos));
    }

    private ExpressionToken symbol() {

        if (pos + 1 < input.length()) {

            String two = input.substring(pos, pos + 2);

            if (SYMBOLS.containsKey(two)) {
                pos += 2;
                return new ExpressionToken(SYMBOLS.get(two), two);
            }
        }

        String one = input.substring(pos, pos + 1);

        pos++;

        ExpressionTokenType t = SYMBOLS.get(one);

        if (t == null)
            throw new RuntimeException("Unknown symbol " + one);

        return new ExpressionToken(t, one);
    }
}
