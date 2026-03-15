package com.example.glue;

import java.util.Map;

public class MapGlue extends AbstractGlue<Map> {
    @Override
    public Object get(Map target, String name) throws Exception {
        return target.get(name);
    }

    @Override
    public Object get(Map target, int index) throws Exception {
        throw new UnsupportedOperationException();
    }
}
