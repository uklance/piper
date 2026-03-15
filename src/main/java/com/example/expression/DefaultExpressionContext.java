package com.example.expression;

import java.util.*;

public class DefaultExpressionContext implements ExpressionContext {

    private final Map<String,Object> vars=new HashMap<>();

    private final MapperRegistry mapperRegistry;
    private final ConverterRegistry converterRegistry;

    public DefaultExpressionContext(
        MapperRegistry mapperRegistry,
        ConverterRegistry converterRegistry
    ){
        this.mapperRegistry=mapperRegistry;
        this.converterRegistry=converterRegistry;
    }

    public void set(String name,Object value){
        vars.put(name,value);
    }

    public Object get(String name){
        return vars.get(name);
    }

    public <T> T convert(Object value,Class<T> type){
        return converterRegistry.convert(value,type);
    }

    public boolean isTruthy(Object value) {
        System.out.println("IsTruthy " + value);
        if (value==null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof String s) return !s.isEmpty();
        return true;
    }

    public Object applyMapper(Object value,String mapperName,List<Object> args){
        return mapperRegistry.apply(value,mapperName,args);
    }
}
