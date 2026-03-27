package com.example.mapper;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class DateTimeFormatMapper implements Mapper<TemporalAccessor> {
    private final Locale locale;
    private final ConcurrentMap<String, DateTimeFormatter> cache = new ConcurrentHashMap<>();

    public DateTimeFormatMapper(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Object apply(TemporalAccessor value, Object[] args) {
        if (args.length != 1) {
            throw new RuntimeException(String.format("Expected 1 arg, found %s: %s", args.length, Arrays.toString(args)));
        }
        String pattern = (String) args[0];
        DateTimeFormatter formatter = cache.computeIfAbsent(pattern, key -> DateTimeFormatter.ofPattern(key, locale));
        return formatter.format(value);
    }
}
