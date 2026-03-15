package com.example.expression;

public interface Mapper<T> {
    Object apply(T value, Object[] args);
}
