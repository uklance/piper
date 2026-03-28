package com.example.glue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultMemberAccess implements MemberAccess {
    private record MethodKey(Class<?> type, String name, int parameterCount) {}
    private record GetterKey(Class<?> type, String name) {}

    private final ConcurrentMap<MethodKey, Method> methodCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<GetterKey, Method> getterCache = new ConcurrentHashMap<>();

    /**
     * General method invocation (used by invoke())
     */
    @Override
    public Object invoke(Object target, String name, Object[] args) throws Exception {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        Class<?> type = target.getClass();
        Method method = methodCache.computeIfAbsent(new MethodKey(type, name, args != null ? args.length : 0), this::findMethod);
        return method.invoke(target, args);
    }

    private Method findMethod(MethodKey key) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : key.type().getDeclaredMethods()) {
            if (method.getName().equals(key.name()) && method.getParameterCount() == key.parameterCount()) {
                candidates.add(method);
            }
        }
        if (candidates.size() != 1) {
            String msg = String.format("Expected a single method in %s named %s with %d parameter(s), found %d",
                    key.type().getName(), key.name(), key.parameterCount(), candidates.size());
            throw new RuntimeException(msg);
        }

        Method method = candidates.getFirst();
        method.setAccessible(true);
        return method;
    }

    /**
     * Bean-style property getter (getXXX / isXXX)
     */
    @Override
    public Object getProperty(Object target, String name) throws Exception {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        Class<?> type = target.getClass();
        Method getter = getterCache.computeIfAbsent(
                new GetterKey(type, name),
                this::findGetter
        );
        return getter.invoke(target);
    }

    private Method findGetter(GetterKey key) {
        String capitalized = capitalize(key.name());
        Set<String> possibleNames = Set.of("get" + capitalized, "is" + capitalized);

        List<Method> candidates = new ArrayList<>();
        for (Method method : key.type().getDeclaredMethods()) {
            if (possibleNames.contains(method.getName()) && method.getParameterCount() == 0) {
                candidates.add(method);
            }
        }

        if (candidates.size() != 1) {
            String msg = String.format("Expected a single getter in %s for property '%s' (%s), found %d",
                    key.type().getName(), key.name(), possibleNames, candidates.size());
            throw new RuntimeException(msg);
        }

        Method method = candidates.getFirst();
        method.setAccessible(true);
        return method;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }    
}
