package com.example.operation;

public class DoubleBinaryOperations implements BinaryOperations<Double> {
    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public Double plus(Double left, Double right) {
        return left.doubleValue() + right.doubleValue();
    }

    @Override
    public Double minus(Double left, Double right) {
        return left.doubleValue() - right.doubleValue();
    }

    @Override
    public Double multiply(Double left, Double right) {
        return left.doubleValue() * right.doubleValue();
    }

    @Override
    public Double divide(Double left, Double right) {
        return left.doubleValue() / right.doubleValue();
    }

    @Override
    public Double mod(Double left, Double right) {
        return left % right;
    }

    @Override
    public boolean equals(Double left, Double right) {
        return left.equals(right);
    }

    @Override
    public int compare(Double left, Double right) {
        return left.compareTo(right);
    }
}
