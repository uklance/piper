package com.example.expression;

public class VariableNode implements Node{

    private final String name;

    public VariableNode(String name){
        this.name=name;
    }

    public Object eval(ExpressionContext ctx){
        return ctx.get(name);
    }
}
