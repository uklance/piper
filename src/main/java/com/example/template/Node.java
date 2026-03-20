package com.example.template;

import com.example.expression.EvalContext;

public interface Node {
    void render(EvalContext context, StringSink sink) throws Exception;
}