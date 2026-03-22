package com.example.operation;

public interface BinaryOperations<T> {
    Class<T> getType();
    T plus(T left, T right);
    T minus(T left, T right);
    T multiply(T left, T right);
    T divide(T left, T right);
}
