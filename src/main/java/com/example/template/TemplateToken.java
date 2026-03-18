package com.example.template;

public class TemplateToken {
    public final TokenType type;
    public final String text;

    public TemplateToken(TokenType type, String text) {
        this.type = type;
        this.text = text;
    }
}
