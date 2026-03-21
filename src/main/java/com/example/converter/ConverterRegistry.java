package com.example.converter;

public interface ConverterRegistry {
    <T> T convert(Object value, Class<T> type);
}
