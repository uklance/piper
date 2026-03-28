package com.example.glue;

import java.util.Map;

public class MapGlue implements Glue<Map> {
    private final MemberAccess memberAccess;

    public MapGlue(MemberAccess memberAccess) {
        this.memberAccess = memberAccess;
    }

    @Override
    public Object get(Map target, String name) throws Exception {
        return target.get(name);
    }

    @Override
    public Object get(Map target, int index) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object invoke(Map target, String name, Object[] args) throws Exception {
        return memberAccess.invoke(target, name, args);
    }
}
