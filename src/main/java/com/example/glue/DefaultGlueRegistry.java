package com.example.glue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultGlueRegistry implements GlueRegistry {
    private record Entry(Class<?> type, Glue glue, int priority) {
    }

    private final List<Entry> registered = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<Class<?>, Glue> cache = new ConcurrentHashMap<>();

    public void register(Class<?> type, Glue glue, int priority) {
        registered.add(new Entry(type, glue, priority));
        cache.clear(); // invalidate cached resolutions
    }

    @Override
    public Glue get(Class<?> type) {
        return cache.computeIfAbsent(type, this::find);
    }

    private Glue find(Class<?> type) {
        Entry best = null;

        for (Entry entry : registered) {
            if (entry.type().isAssignableFrom(type)) {
                if (best == null || entry.priority() > best.priority()) {
                    best = entry;
                    continue;
                }

                if (entry.priority() == best.priority()) {
                    String msg = String.format("Multiple AccessGlue with priority %s for type %s", entry.priority, type.getName());
                    throw new RuntimeException(msg);
                }
            }
        }
        if (best == null) {
            throw new RuntimeException("No AccessGlue for " + type.getName());
        }
        return best.glue();
    }
}