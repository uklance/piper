package com.example.glue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BeanGlue extends AbstractGlue {
    private record Key(Class<?> type, String name) {}
    private final ConcurrentMap<Key, Method> getCache = new ConcurrentHashMap<>();

    @Override
    public Object get(Object target, String name) throws Exception {
        Method getter = getCache.computeIfAbsent(new Key(target.getClass(), name), key -> {
            Set<String> methodNames = Set.of("is" + capitalize(key.name), "get" + capitalize(key.name));
            List<Method> candidates = new ArrayList<>();
            for (Method method : key.type.getDeclaredMethods()) {
                if (methodNames.contains(method.getName())  && method.getParameterCount() == 0) {
                    candidates.add(method);
                }
            }
            if (candidates.size() != 1) {
                String msg = String.format("Expected a single method in %s named %s with 0 parameter(s) found %s",
                        key.type.getName(), methodNames, candidates.size());
                throw new RuntimeException(msg);
            }
            Method method = candidates.getFirst();
            method.setAccessible(true);
            return method;
        });
        return getter.invoke(target);
    }

    @Override
    public Object get(Object target, int index) {
        throw new UnsupportedOperationException();
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
