package com.example.operation;

public class IntegerBinaryOperations implements BinaryOperations<Integer> {
    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public Integer plus(Integer left, Integer right) {
        return left.intValue() + right.intValue();
    }

    @Override
    public Integer minus(Integer left, Integer right) {
        return left.intValue() - right.intValue();
    }

    @Override
    public Integer multiply(Integer left, Integer right) {
        return left.intValue() * right.intValue();
    }

    @Override
    public Integer divide(Integer left, Integer right) {
        return left.intValue() / right.intValue();
    }
}
