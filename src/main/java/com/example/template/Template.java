package com.example.template;

import com.example.expression.EvalContext;

import java.io.Writer;

public interface Template {
    void apply(EvalContext context, StringSink sink) throws Exception;

    default String apply(EvalContext context) throws Exception {
        StringBuilder builder = new StringBuilder();
        apply(context, builder::append);
        return builder.toString();
    }

    default void apply(EvalContext context, Writer writer) throws Exception {
        apply(context, writer::write);
    }
}
