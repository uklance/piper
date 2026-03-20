package com.example.template;

import com.example.expression.ExpressionContext;

import java.io.Writer;

public interface Template {
    void apply(ExpressionContext context, StringSink sink) throws Exception;

    default String apply(ExpressionContext context) throws Exception {
        StringBuilder builder = new StringBuilder();
        apply(context, builder::append);
        return builder.toString();
    }

    default void apply(ExpressionContext context, Writer writer) throws Exception {
        apply(context, writer::write);
    }
}
