package com.example.expression;

public enum ExpressionTokenType {

    NUMBER,
    STRING,
    IDENTIFIER,

    PLUS,
    MINUS,
    STAR,
    SLASH,

    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,

    DOT,
    SAFE_DOT,

    COMMA,

    QUESTION,
    COLON,

    PIPE,

    EQ,
    NE,
    LT,
    GT,
    LE,
    GE,

    AND,
    OR,

    EOF,

    MOD;
}
