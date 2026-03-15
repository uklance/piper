package com.example.expression;

import java.lang.reflect.*;
import java.util.concurrent.*;

public class MethodCache {

    private static final ConcurrentHashMap<String,Method> CACHE=new ConcurrentHashMap<>();

    public static Method lookup(Class<?> c,String name,int argc){

        String key=c.getName()+"#"+name+"#"+argc;

        return CACHE.computeIfAbsent(key,k->{

            for(Method m:c.getMethods()){

                if(!m.getName().equals(name)) continue;

                if(m.getParameterCount()==argc)
                    return m;
            }

            throw new RuntimeException("Method not found "+name);
        });
    }
}
