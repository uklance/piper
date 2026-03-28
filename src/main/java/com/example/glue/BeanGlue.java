package com.example.glue;

public class BeanGlue implements Glue<Object> {
    private final MemberAccess memberAccess;

    public BeanGlue(MemberAccess memberAccess) {
        this.memberAccess = memberAccess;
    }

    @Override
    public Object get(Object target, String name) throws Exception {
        return memberAccess.getProperty(target, name);
    }

    @Override
    public Object get(Object target, int index) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object invoke(Object target, String name, Object[] args) throws Exception {
        return memberAccess.invoke(target, name, args);
    }
}
