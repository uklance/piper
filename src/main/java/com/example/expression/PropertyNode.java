package com.example.expression;

public class PropertyNode implements Node {
    private final Node targetNode;
    private final String name;
    private final boolean safe;

    public PropertyNode(Node target, String name, boolean safe) {
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
