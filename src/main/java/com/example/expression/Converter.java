package com.example.expression;

public interface Converter<F,T> {

    T convert(F value);

}
