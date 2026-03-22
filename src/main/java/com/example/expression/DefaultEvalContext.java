package com.example.expression;

import com.example.converter.ConverterRegistry;
import com.example.glue.GlueRegistry;
import com.example.mapper.MapperRegistry;
import com.example.operation.BinaryOperationRegistry;
import com.example.operation.BinaryOperations;

import java.util.HashMap;
import java.util.Map;

public class DefaultEvalContext implements EvalContext {
    private final Map<String, Object> values = new HashMap<>();

    private final MapperRegistry mapperRegistry;
    private final ConverterRegistry converterRegistry;
    private final GlueRegistry glueRegistry;
    private final BinaryOperationRegistry binaryOpsRegistry;

    public DefaultEvalContext(
            MapperRegistry mapperRegistry,
            ConverterRegistry converterRegistry,
            GlueRegistry glueRegistry,
            BinaryOperationRegistry binaryOpsRegistry) {
        this.mapperRegistry = mapperRegistry;
        this.converterRegistry = converterRegistry;
        this.glueRegistry = glueRegistry;
        this.binaryOpsRegistry = binaryOpsRegistry;
    }

    @Override
    public void setValue(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public Object getValue(String name) {
        return values.get(name);
    }

    @Override
    public <T> T convert(Object value, Class<T> type) {
        return converterRegistry.convert(value, type);
    }

    @Override
    public boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof String s) return !s.isEmpty();
        return true;
    }

    @Override
    public Object applyMapper(Object value, String mapperName, Object[] args) {
        return mapperRegistry.apply(value, mapperName, args);
    }

    @Override
    public Object get(Object target, String name) throws Exception {
        return glueRegistry.get(target.getClass()).get(target, name);
    }

    @Override
    public Object get(Object target, int index) throws Exception {
        return glueRegistry.get(target.getClass()).get(target, index);
    }

    @Override
    public Object invoke(Object target, String name, Object[] args) throws Exception {
        return glueRegistry.get(target.getClass()).invoke(target, name, args);
    }

    @Override
    public <T> BinaryOperations<T> getBinaryOperations(Class<T> type) {
        return binaryOpsRegistry.get(type);
    }
}
