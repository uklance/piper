package com.example.expression;

public interface Node {
    Object eval(ExpressionContext ctx) throws Exception;
}
