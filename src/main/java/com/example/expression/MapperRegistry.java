package com.example.expression;

import java.util.List;

public interface MapperRegistry {

    <T> void register(Class<T> type, String name, Mapper<T> mapper);

    Mapper<?> get(Class<?> type, String name);

    Object apply(Object value, String mapperName, List<Object> args);
}
