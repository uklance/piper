package com.example.expression;

public class ExpressionToken {

    public final ExpressionTokenType type;
    public final String text;

    public ExpressionToken(ExpressionTokenType type, String text) {
        this.type = type;
        this.text = text;
    }
}
