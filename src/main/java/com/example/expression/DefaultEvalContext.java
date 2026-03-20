package com.example.expression;

import com.example.converter.ConverterRegistry;
import com.example.glue.GlueRegistry;
import com.example.mapper.MapperRegistry;

import java.util.HashMap;
import java.util.Map;

public class DefaultEvalContext implements EvalContext {
    private final Map<String, Object> vars = new HashMap<>();

    private final MapperRegistry mapperRegistry;
    private final ConverterRegistry converterRegistry;
    private final GlueRegistry glueRegistry;

    public DefaultEvalContext(
            MapperRegistry mapperRegistry,
            ConverterRegistry converterRegistry,
            GlueRegistry glueRegistry
    ) {
        this.mapperRegistry = mapperRegistry;
        this.converterRegistry = converterRegistry;
        this.glueRegistry = glueRegistry;
    }

    public void set(String name, Object value) {
        vars.put(name, value);
    }

    public Object get(String name) {
        return vars.get(name);
    }

    public <T> T convert(Object value, Class<T> type) {
        return converterRegistry.convert(value, type);
    }

    public boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        if (value instanceof String s) return !s.isEmpty();
        return true;
    }

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
}
