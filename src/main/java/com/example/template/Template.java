package com.example.template;

import com.example.expression.ExpressionContext;

import java.io.IOException;
import java.io.Writer;

public interface Template {
    void apply(ExpressionContext context, StringSink sink) throws IOException;
    default String apply(ExpressionContext context) throws IOException {
        StringBuilder builder = new StringBuilder();
        apply(context, builder::append);
        return builder.toString();
    }
    default void apply(ExpressionContext context, Writer writer) throws IOException {
        apply(context, writer::write);
    }
}
