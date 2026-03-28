package com.example.template;

public class TemplateToken {
    public final TemplateTokenType type;
    public final String text;

    public TemplateToken(TemplateTokenType type, String text) {
        this.type = type;
        this.text = text;
    }
}
