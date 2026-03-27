package com.example.converter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultConverterRegistry implements ConverterRegistry {
    private record Key(Class<?> from, Class<?> to) {}
    private final Map<Key, Converter<?, ?>> converters = new HashMap<>();
    private final ConcurrentMap<Key, Converter<?, ?>> cache = new ConcurrentHashMap<>();

    // Placeholder as ConcurrentHashMap does not support null
    private static final Converter<?, ?> NULL_CONVERTER = v -> {
        throw new IllegalStateException("No NULL_CONVERTER");
    };

    private static final Converter<?, ?> IDENTITY_CONVERTER = v -> v;

    public <F, T> void register(Class<F> from, Class<T> to, Converter<F, T> converter) {
        converters.put(new Key(from, to), converter);
        cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(Object value, Class<T> type) {
        if (value == null) return null;
        Key key = new Key(value.getClass(), type);
        Converter<Object, T> converter = (Converter<Object, T>) cache.computeIfAbsent(key, k -> this.find(k.from, k.to));
        if (converter == NULL_CONVERTER) {
            throw new RuntimeException(String.format("Converter not found %s -> %s", key.from().getName(), type.getName()));
        }
        return converter.convert(value);
    }

    @SuppressWarnings("unchecked")
    private Converter<?, ?> find(Class<?> from, Class<?> to) {
        if (from.equals(to)) {
            return IDENTITY_CONVERTER;
        }

        // breadth first search over class hierarchy & interfaces
        Queue<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new LinkedHashSet<>();
        queue.add(from);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            Converter<?, ?> converter = converters.get(new Key(current, to));
            if (converter != null) {
                return converter;
            }
            Class<?> superclass = current.getSuperclass();
            if (superclass != null && visited.add(superclass)) {
                queue.add(superclass);
            }
            for (Class<?> iface : current.getInterfaces()) {
                if (visited.add(iface)) {
                    queue.add(iface);
                }
            }
        }

        if (to.isAssignableFrom(from)) {
            return IDENTITY_CONVERTER;
        }

        //  ConcurrentHashMap does not support null
        return NULL_CONVERTER;
    }
}