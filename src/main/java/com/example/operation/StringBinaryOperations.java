package com.example.operation;

public class StringBinaryOperations implements BinaryOperations<String> {
    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public String plus(String left, String right) {
        return left + right;
    }

    @Override
    public String minus(String left, String right) {
        throw new UnsupportedOperationException("minus(String, String)");
    }

    @Override
    public String multiply(String left, String right) {
        throw new UnsupportedOperationException("multiply(String, String)");
    }

    @Override
    public String divide(String left, String right) {
        throw new UnsupportedOperationException("divide(String, String)");
    }

    @Override
    public String mod(String left, String right) {
        throw new UnsupportedOperationException("mod(String, String)");
    }

    @Override
    public boolean equals(String left, String right) {
        return left.equals(right);
    }

    @Override
    public int compare(String left, String right) {
        return left.compareTo(right);
    }
}
