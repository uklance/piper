package com.example.glue;

import java.util.List;

public interface Glue<T> {
    Object get(T target, String name) throws Exception;
    Object get(T target, int index) throws Exception;
    Object invoke(T target, String name, Object[] args) throws Exception;
}
