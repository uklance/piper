package com.example.mapper;

import java.util.*;

public class DefaultMapperRegistry implements MapperRegistry {
    private final Map<Class<?>, Map<String, Mapper<?>>> registry = new HashMap<>();

    public <T> void register(Class<T> type, String name, Mapper<T> mapper) {
        registry
                .computeIfAbsent(type, k -> new HashMap<>())
                .put(name, mapper);
    }

    private Mapper<?> find(Class<?> type, String name) {
        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();

        queue.add(type);

        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();

            Mapper<?> mapper = findDirect(current, name);
            if (mapper != null) {
                return mapper;
            }

            Class<?> superClass = current.getSuperclass();
            if (superClass != null && visited.add(superClass)) {
                queue.add(superClass);
            }

            for (Class<?> iface : current.getInterfaces()) {
                if (visited.add(iface)) {
                    queue.add(iface);
                }
            }
        }
        return null;
    }

    private Mapper<?> findDirect(Class<?> type, String name) {
        Map<String, Mapper<?>> m = registry.get(type);
        if (m == null) return null;
        return m.get(name);
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
