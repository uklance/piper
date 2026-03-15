package com.example.expression;

public class Expression {

    private final Node root;

    public Expression(Node root){
        this.root=root;
    }

    public Object eval(ExpressionContext ctx) throws Exception {
        return root.eval(ctx);
    }

    public <T> T eval(ExpressionContext ctx, Class<T> type) throws Exception {
        return ctx.convert(root.eval(ctx), type);
    }
}
