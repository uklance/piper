package com.example.glue;

public interface MemberAccess {
    Object invoke(Object target, String name, Object[] args) throws Exception;
    Object getProperty(Object target, String name) throws Exception;
}