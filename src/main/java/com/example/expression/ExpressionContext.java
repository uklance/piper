package com.example.expression;

public interface ExpressionContext {

    Object get(String name);

    void set(String name,Object value);

    <T> T convert(Object value, Class<T> type);

    boolean isTruthy(Object value);

    Object applyMapper(Object value, String mapperName, Object[] args);

    Object get(Object target, String name) throws Exception;
    Object get(Object target, int index) throws Exception;
    Object invoke(Object target, String name, Object[] args) throws Exception;
}
