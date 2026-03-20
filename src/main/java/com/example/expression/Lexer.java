package com.example.expression;

import java.util.HashMap;
import java.util.Map;

public class Lexer {

    private final String input;
    private int pos;

    private static final Map<String, TokenType> SYMBOLS = new HashMap<>();

    static {

        SYMBOLS.put("+", TokenType.PLUS);
        SYMBOLS.put("-", TokenType.MINUS);
        SYMBOLS.put("*", TokenType.STAR);
        SYMBOLS.put("/", TokenType.SLASH);
        SYMBOLS.put("(", TokenType.LPAREN);
        SYMBOLS.put(")", TokenType.RPAREN);
        SYMBOLS.put(".", TokenType.DOT);
        SYMBOLS.put(",", TokenType.COMMA);
        SYMBOLS.put("?", TokenType.QUESTION);
        SYMBOLS.put(":", TokenType.COLON);
        SYMBOLS.put("|", TokenType.PIPE);
        SYMBOLS.put("[", TokenType.LBRACKET);
        SYMBOLS.put("]", TokenType.RBRACKET);
        SYMBOLS.put("&&", TokenType.AND);
        SYMBOLS.put("||", TokenType.OR);
        SYMBOLS.put("==", TokenType.EQ);
        SYMBOLS.put("!=", TokenType.NE);
        SYMBOLS.put("<", TokenType.LT);
        SYMBOLS.put(">", TokenType.GT);
        SYMBOLS.put("<=", TokenType.LE);
        SYMBOLS.put(">=", TokenType.GE);
        SYMBOLS.put("?.", TokenType.SAFE_DOT);
    }

    public Lexer(String input) {
        this.input = input;
    }

    public Token peek() {
        int save = pos;
        Token t = next();
        pos = save;
        return t;
    }

    public Token next() {

        skip();

        if (pos >= input.length())
            return new Token(TokenType.EOF, "");

        char c = input.charAt(pos);

        if (Character.isDigit(c))
            return number();

        if (c == '\'')
            return string();

        if (Character.isLetter(c) || c == '_')
            return identifier();

        return symbol();
    }

    public Token next(TokenType t) {

        Token n = next();

        if (n.type != t)
            throw new RuntimeException("Expected " + t + " got " + n.type);

        return n;
    }

    private void skip() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
            pos++;
    }

    private Token number() {

        int start = pos;

        while (pos < input.length() &&
                (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
            pos++;

        return new Token(TokenType.NUMBER, input.substring(start, pos));
    }

    private Token string() {

        pos++;

        int start = pos;

        while (input.charAt(pos) != '\'')
            pos++;

        String s = input.substring(start, pos);

        pos++;

        return new Token(TokenType.STRING, s);
    }

    private Token identifier() {

        int start = pos;

        while (pos < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
            pos++;

        return new Token(TokenType.IDENTIFIER, input.substring(start, pos));
    }

    private Token symbol() {

        if (pos + 1 < input.length()) {

            String two = input.substring(pos, pos + 2);

            if (SYMBOLS.containsKey(two)) {
                pos += 2;
                return new Token(SYMBOLS.get(two), two);
            }
        }

        String one = input.substring(pos, pos + 1);

        pos++;

        TokenType t = SYMBOLS.get(one);

        if (t == null)
            throw new RuntimeException("Unknown symbol " + one);

        return new Token(t, one);
    }
}
