package com.example.template;

import java.io.IOException;
import java.io.Reader;

public class TemplateLexer {
    private final String template;
    private int pos;
    private TemplateToken next;

    public TemplateLexer(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192]; // 8KB
        int n;
        while ((n = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, n);
        }
        this.template = builder.toString();
        this.next = parseNext();
    }

    public TemplateLexer(String template) {
        this.template = template;
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
        return template.startsWith(str, pos);
    }

    private TemplateToken parseNext() {
        if (pos >= template.length()) {
            return new TemplateToken(TokenType.EOF, "");
        }

        // -------- INTERPOLATION --------
        if (startsWith("${")) {
            int start = pos + 2;
            int end = template.indexOf('}', start);
            if (end < 0) throw new RuntimeException("Unclosed interpolation");

            String expr = template.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.INTERPOLATION, expr);
        }

        // -------- DIRECTIVE START (<#...>) --------
        if (startsWith("<#")) {
            int start = pos + 2;
            int end = template.indexOf('>', start);
            if (end < 0) throw new RuntimeException("Unclosed directive start");

            String name = template.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.DIRECTIVE_START, name);
        }

        // -------- DIRECTIVE END (</#...>) --------
        if (startsWith("</#")) {
            int start = pos + 3;
            int end = template.indexOf('>', start);
            if (end < 0) throw new RuntimeException("Unclosed directive end");

            String name = template.substring(start, end).trim();
            pos = end + 1;
            return new TemplateToken(TokenType.DIRECTIVE_END, name);
        }

        // -------- TEXT --------
        int start = pos;
        while (pos < template.length() && !startsWith("${") && !startsWith("<#") && !startsWith("</#")) {
            pos++;
        }

        return new TemplateToken(TokenType.TEXT, template.substring(start, pos));
    }
}
