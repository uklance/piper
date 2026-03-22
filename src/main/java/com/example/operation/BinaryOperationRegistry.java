package com.example.operation;

public interface BinaryOperationRegistry {
    <T> BinaryOperations<T> get(Class<T> type);
}
