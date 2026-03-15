package com.example.expression;

import java.util.*;

public class DefaultMapperRegistry implements MapperRegistry {

    private final Map<Class<?>, Map<String, Mapper<?>>> registry = new HashMap<>();

    public <T> void register(Class<T> type, String name, Mapper<T> mapper) {

        registry
            .computeIfAbsent(type,k->new HashMap<>())
            .put(name,mapper);
    }

    public Mapper<?> get(Class<?> type, String name) {

        Map<String,Mapper<?>> m = registry.get(type);

        if (m == null) return null;

        return m.get(name);
    }

    public Object apply(Object value, String mapperName, List<Object> args) {

        if (value == null) return null;

        Mapper mapper = get(value.getClass(),mapperName);

        if (mapper == null) {
            throw new RuntimeException("Mapper not found: "+mapperName);
        }

        return mapper.apply(value,args);
    }
}
