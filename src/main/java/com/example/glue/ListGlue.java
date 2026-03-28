package com.example.glue;

import java.util.List;

public class ListGlue implements Glue<List> {
    private final MemberAccess memberAccess;

    public ListGlue(MemberAccess memberAccess) {
        this.memberAccess = memberAccess;
    }

    @Override
    public Object get(List target, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(List target, int index) {
        return target.get(index);
    }

    @Override
    public Object invoke(List target, String name, Object[] args) throws Exception {
        return memberAccess.invoke(target, name, args);
    }
}
