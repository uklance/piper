package com.example.glue;

import java.util.List;

public class ListGlue extends AbstractGlue<List> {
    @Override
    public Object get(List target, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(List target, int index) {
        return target.get(index);
    }
}
