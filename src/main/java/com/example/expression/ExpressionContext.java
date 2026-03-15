package com.example.expression;

import java.util.List;

public interface ExpressionContext {

    Object get(String name);

    void set(String name,Object value);

    <T> T convert(Object value, Class<T> type);

    boolean isTruthy(Object value);

    Object applyMapper(Object value, String mapperName, List<Object> args);
}
