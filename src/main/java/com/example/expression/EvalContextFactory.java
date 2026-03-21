package com.example.expression;

import java.util.Map;

public interface EvalContextFactory {
    EvalContext create(Map<String, ?> values);
}
