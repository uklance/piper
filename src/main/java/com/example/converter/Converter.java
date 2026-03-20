package com.example.converter;

public interface Converter<F,T> {

    T convert(F value);

}
