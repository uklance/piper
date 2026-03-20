package com.example.mapper;

public interface Mapper<T> {
    Object apply(T value, Object[] args);
}
