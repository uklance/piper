package com.example.mapper;

import com.example.glue.MemberAccess;

import java.util.*;

public class DefaultMapperRegistry implements MapperRegistry {
    private final MemberAccess memberAccess;

    public DefaultMapperRegistry(MemberAccess memberAccess) {
        this.memberAccess = memberAccess;
    }

    private final Map<Class<?>, Map<String, Mapper<?>>> registry = new HashMap<>();

    public <T> void register(Class<T> type, String name, Mapper<T> mapper) {
        registry
                .computeIfAbsent(type, k -> new HashMap<>())
                .put(name, mapper);
    }

    private Mapper<?> find(Class<?> type, String name) {
        for (Class<?> current : memberAccess.getHierarchy(type)) {
            Map<String, Mapper<?>> typeMappers = registry.get(current);
            if (typeMappers != null) {
                Mapper<?> mapper = typeMappers.get(name);
                if (mapper != null) {
                    return mapper;
                }
            }
        }
        return null;
    }

    @Override
    public Object apply(Object value, String mapperName, Object[] args) {
        if (value == null) return null;
        Mapper mapper = find(value.getClass(), mapperName);
        if (mapper == null) {
            throw new RuntimeException(String.format("Mapper '%s' not registered for %s", mapperName, value.getClass().getName()));
        }
        return mapper.apply(value, args);
    }
}
