package com.example.template;

import com.example.expression.EvalContext;

import java.io.Writer;
import java.util.Map;

public interface Template {
    void apply(EvalContext context, StringSink sink) throws Exception;

    void apply(Map<String, ?> context, StringSink sink) throws Exception;

    default String apply(Map<String, ?> context) throws Exception {
        StringBuilder builder = new StringBuilder();
        apply(context, builder::append);
        return builder.toString();
    }

    default void apply(Map<String, ?> context, Writer writer) throws Exception {
        apply(context, writer::write);
    }
}
