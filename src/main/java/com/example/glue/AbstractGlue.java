package com.example.glue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractGlue<T> implements Glue<T> {
    private record Key(Class<?> type, String name, int parameterCount) {
    }

    private final ConcurrentMap<Key, Method> cache = new ConcurrentHashMap<>();

    @Override
    public Object invoke(T target, String name, Object[] args) throws Exception {
        Class<?> type = target.getClass();
        Method method = cache.computeIfAbsent(new Key(type, name, args.length), this::findMethod);
        return method.invoke(target, args);
    }

    private Method findMethod(Key key) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : key.type.getDeclaredMethods()) {
            if (method.getName().equals(key.name) && method.getParameterCount() == key.parameterCount) {
                candidates.add(method);
            }
        }
        if (candidates.size() != 1) {
            String msg = String.format("Expected a single method in %s named %s with %s parameter(s) found %s",
                    key.type.getName(), key.name, key.parameterCount, candidates.size());
            throw new RuntimeException(msg);
        }
        Method method = candidates.getFirst();
        method.setAccessible(true);
        return method;
    }
}
