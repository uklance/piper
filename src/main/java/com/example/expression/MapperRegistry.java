package com.example.expression;

public interface MapperRegistry {
    Object apply(Object value, String mapperName, Object[] args);
}
