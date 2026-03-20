package com.example.converter;

public interface ConverterRegistry {

    <F,T> void register(Class<F> from, Class<T> to, Converter<F,T> converter);

    <T> T convert(Object value, Class<T> type);
}
