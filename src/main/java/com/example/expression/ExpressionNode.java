package com.example.expression;

public interface ExpressionNode {
    Object eval(EvalContext ctx) throws Exception;
}
