package com.example.converter;

import java.util.HashMap;
import java.util.Map;

public class DefaultConverterRegistry implements ConverterRegistry {

    private static class Key {
        Class from;
        Class to;

        Key(Class f, Class t) {
            from = f;
            to = t;
        }

        public int hashCode() {
            return from.hashCode() * 31 + to.hashCode();
        }

        public boolean equals(Object o) {
            Key k = (Key) o;
            return from.equals(k.from) && to.equals(k.to);
        }
    }

    private final Map<Key, Converter> converters = new HashMap<>();

    public <F, T> void register(Class<F> from, Class<T> to, Converter<F, T> converter) {
        converters.put(new Key(from, to), converter);
    }

    public <T> T convert(Object value, Class<T> type) {

        if (value == null) return null;

        Converter c = converters.get(new Key(value.getClass(), type));
        if (c != null) {
            return type.cast(c.convert(value));
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new RuntimeException(String.format("Converter not found %s -> %s", value.getClass().getName(), type.getName()));
    }
}
