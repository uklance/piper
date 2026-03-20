package com.example.expression;

public interface Node {
    Object eval(EvalContext ctx) throws Exception;
}
