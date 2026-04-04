package com.example.expression;

import java.util.HashMap;
import java.util.Map;

public class ExpressionLexer {

    private final String input;
    private int pos;

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

        if (c == '\'' || c == '"')
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

        char quote = input.charAt(pos++); // ' or "

        StringBuilder sb = new StringBuilder();

        while (pos < input.length()) {
            char c = input.charAt(pos++);

            if (c == quote)
                break;

            if (c == '\\') { // escape support
                char next = input.charAt(pos++);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\'': sb.append('\''); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }

        return new ExpressionToken(ExpressionTokenType.STRING, sb.toString());
    }

    private ExpressionToken identifier() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }

        String text = input.substring(start, pos);
        switch (text) {
            case "gt": return token(ExpressionTokenType.GT, text);
            case "lt": return token(ExpressionTokenType.LT, text);
            case "gte": return token(ExpressionTokenType.GE, text);
            case "lte": return token(ExpressionTokenType.LE, text);
        }

        return new ExpressionToken(ExpressionTokenType.IDENTIFIER, text);
    }

    private ExpressionToken symbol() {
        char c = input.charAt(pos);

        switch (c) {
            case '+': pos++; return token(ExpressionTokenType.PLUS, "+");
            case '-': pos++; return token(ExpressionTokenType.MINUS, "-");
            case '*': pos++; return token(ExpressionTokenType.STAR, "*");
            case '/': pos++; return token(ExpressionTokenType.SLASH, "/");
            case '%': pos++; return token(ExpressionTokenType.MOD, "%");
            case '(': pos++; return token(ExpressionTokenType.LPAREN, "(");
            case ')': pos++; return token(ExpressionTokenType.RPAREN, ")");
            case '[': pos++; return token(ExpressionTokenType.LBRACKET, "[");
            case ']': pos++; return token(ExpressionTokenType.RBRACKET, "]");
            case '{': pos++; return token(ExpressionTokenType.LBRACE, "{");
            case '}': pos++; return token(ExpressionTokenType.RBRACE, "}");
            case ',': pos++; return token(ExpressionTokenType.COMMA, ",");
            case ':': pos++; return token(ExpressionTokenType.COLON, ":");
            case '.': pos++; return token(ExpressionTokenType.DOT, ".");
            case '?':
                if (peekChar('.')) {
                    pos += 2;
                    return token(ExpressionTokenType.SAFE_DOT, "?.");
                }
                pos++;
                return token(ExpressionTokenType.QUESTION, "?");

            case '&':
                expectNext('&');
                return token(ExpressionTokenType.AND, "&&");

            case '|':
                if (peekChar('|')) {
                    pos += 2;
                    return token(ExpressionTokenType.OR, "||");
                }
                pos++;
                return token(ExpressionTokenType.PIPE, "|");

            case '=':
                expectNext('=');
                return token(ExpressionTokenType.EQ, "==");

            case '!':
                expectNext('=');
                return token(ExpressionTokenType.NE, "!=");

            case '>':
                if (peekChar('=')) {
                    pos += 2;
                    return token(ExpressionTokenType.GE, ">=");
                }
                pos++;
                return token(ExpressionTokenType.GT, ">");

            case '<':
                if (peekChar('=')) {
                    pos += 2;
                    return token(ExpressionTokenType.LE, "<=");
                }
                pos++;
                return token(ExpressionTokenType.LT, "<");
        }

        throw new RuntimeException("Unknown symbol: " + c);
    }

    private boolean peekChar(char expected) {
        return pos + 1 < input.length() && input.charAt(pos + 1) == expected;
    }

    private void expectNext(char expected) {
        if (!peekChar(expected))
            throw new RuntimeException("Expected '" + expected + "'");
        pos += 2;
    }

    private ExpressionToken token(ExpressionTokenType type, String text) {
        return new ExpressionToken(type, text);
    }
}
