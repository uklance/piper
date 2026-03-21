package com.example.expression;

import java.util.Map;

public interface EvalContext {
    Map<String, Object> getValues();

    Object getValue(String name);

    void setValue(String name, Object value);

    <T> T convert(Object value, Class<T> type);

    boolean isTruthy(Object value);

    Object applyMapper(Object value, String mapperName, Object[] args);

    Object get(Object target, String name) throws Exception;

    Object get(Object target, int index) throws Exception;

    Object invoke(Object target, String name, Object[] args) throws Exception;
}
