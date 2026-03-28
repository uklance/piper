package com.example.expression;

public class PropertyExpressionNode implements ExpressionNode {
    private final ExpressionNode targetNode;
    private final String name;
    private final boolean safe;

    public PropertyExpressionNode(ExpressionNode target, String name, boolean safe) {
        this.targetNode = target;
        this.name = name;
        this.safe = safe;
    }

    public Object eval(EvalContext ctx) throws Exception {
        Object target = targetNode.eval(ctx);
        if (target == null) {
            if (safe) return null;
            throw new RuntimeException("Null property access");
        }
        return ctx.get(target, name);
    }
}
