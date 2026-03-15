package com.example.template;

import com.example.expression.ExpressionContext;

import java.io.IOException;

public interface Node {
    void render(ExpressionContext context, StringSink sink) throws IOException;
}