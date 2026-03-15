package com.example.expression;

import java.util.List;

public interface Mapper<T> {

    Object apply(T value, List<Object> args);

}
