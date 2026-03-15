package com.example.template;

import com.example.expression.ExpressionContext;

public interface Node {
    void render(ExpressionContext context, StringSink sink) throws Exception;
}