package com.example.expression;

import java.lang.reflect.*;

public class PropertyNode implements Node{

    private final Node target;
    private final String name;
    private final boolean safe;

    public PropertyNode(Node target,String name,boolean safe){
        this.target =target;
        this.name =name;
        this.safe =safe;
    }

    public Object eval(ExpressionContext ctx){

        Object obj=target.eval(ctx);

        if(obj==null){
            if(safe) return null;
            throw new RuntimeException("Null property access");
        }

        try{

            Field f=obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);

        }catch(Exception e){

            try{

                Method m=obj.getClass().getMethod("get"+cap(name));
                return m.invoke(obj);

            }catch(Exception ex){
                throw new RuntimeException(ex);
            }
        }
    }

    private String cap(String s){
        return Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
}
