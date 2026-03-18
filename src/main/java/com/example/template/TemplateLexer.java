package com.example.template;

public class TemplateLexer {
    private final String s;
    private int pos;
    private TemplateToken next;

    public TemplateLexer(String s) {
        this.s = s;
        this.next = parseNext();
    }

    public TemplateToken peek() {
        return next;
    }

    public TokenType peekType() {
        return next.type;
    }

    public TemplateToken next() {
        TemplateToken t = next;
        next = parseNext();
        return t;
    }

    public TemplateToken next(TokenType expected) {
        if (next.type != expected) {
            throw new RuntimeException("Expected " + expected + " but got " + next.type);
        }
        return next();
    }

    private boolean startsWith(String str) {
        return s.startsWith(str, pos);
    }

    private TemplateToken parseNext() {
        if (pos >= s.length()) {
            return new TemplateToken(TokenType.EOF, "");
        }

        // -------- INTERPOLATION --------
        if (startsWith("${")) {
            int start = pos + 2;
            int end = s.indexOf('}', start);
            if (end < 0) throw new RuntimeException("Unclosed interpolation");

            String expr = s.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.INTERPOLATION, expr);
        }

        // -------- DIRECTIVE START (<#...>) --------
        if (startsWith("<#")) {
            int start = pos + 2;
            int end = s.indexOf('>', start);
            if (end < 0) throw new RuntimeException("Unclosed directive start");

            String name = s.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.DIRECTIVE_START, name);
        }

        // -------- DIRECTIVE END (</#...>) --------
        if (startsWith("</#")) {
            int start = pos + 3;
            int end = s.indexOf('>', start);
            if (end < 0) throw new RuntimeException("Unclosed directive end");

            String name = s.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.DIRECTIVE_END, name);
        }

        // -------- TEXT --------
        int start = pos;
        while (pos < s.length() && !startsWith("${") && !startsWith("<#") && !startsWith("</#")) {
            pos++;
        }

        return new TemplateToken(TokenType.TEXT, s.substring(start, pos));
    }
}
