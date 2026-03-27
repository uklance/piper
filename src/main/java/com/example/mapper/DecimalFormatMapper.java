package com.example.mapper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DecimalFormatMapper implements Mapper<Number> {
    private final DecimalFormatSymbols symbols;
    private final ThreadLocal<Map<String, DecimalFormat>> cacheLocal = ThreadLocal.withInitial(() -> new HashMap<>());

    public DecimalFormatMapper(DecimalFormatSymbols symbols) {
        this.symbols = symbols;
    }

    @Override
    public Object apply(Number value, Object[] args) {
        if (args.length != 1) {
            throw new RuntimeException(String.format("Expected 1 arg, found %s: %s", args.length, Arrays.toString(args)));
        }
        String pattern = (String) args[0];
        DecimalFormat format = cacheLocal.get().computeIfAbsent(pattern, key -> new DecimalFormat(key, symbols));
        return format.format(value);
    }
}
