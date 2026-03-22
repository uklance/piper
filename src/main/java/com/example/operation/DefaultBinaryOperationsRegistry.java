package com.example.operation;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultBinaryOperationsRegistry implements BinaryOperationRegistry {
    private final Map<Class<?>, BinaryOperations<?>> registry = new LinkedHashMap<>();

    @Override
    public <T> BinaryOperations<T> get(Class<T> type) {
        BinaryOperations binaryOps = registry.get(type);
        if (binaryOps == null) {
            throw new RuntimeException("No binary operations registered for " + type.getName());
        }
        return binaryOps;
    }

    public <T> void register(Class<T> type, BinaryOperations<T> binaryOps) {
        registry.put(type, binaryOps);
    }
}
