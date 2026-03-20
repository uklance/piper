package com.example.mapper;

public interface MapperRegistry {
    Object apply(Object value, String mapperName, Object[] args);
}
