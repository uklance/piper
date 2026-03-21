package com.example;

import com.example.converter.Converter;
import com.example.converter.ConverterRegistry;
import com.example.converter.DefaultConverterRegistry;
import com.example.expression.DefaultEvalContext;
import com.example.expression.EvalContext;
import com.example.expression.ExpressionParser;
import com.example.glue.*;
import com.example.mapper.DefaultMapperRegistry;
import com.example.mapper.Mapper;
import com.example.mapper.MapperRegistry;
import com.example.template.ReaderSource;
import com.example.template.Template;
import com.example.template.TemplateLexer;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Piper {
    public static class Builder {
        private final DefaultMapperRegistry mappers = new DefaultMapperRegistry();
        private final DefaultConverterRegistry converters = new DefaultConverterRegistry();
        private final DefaultGlueRegistry glues = new DefaultGlueRegistry();
        private ReaderSource readerSource;

        public <T> Builder withMapper(Class<T> type, String name, Mapper<T> mapper) {
            mappers.register(type, name, mapper);
            return this;
        }

        public <F, T> Builder withConverter(Class<F> from, Class<T> to, Converter<F, T> converter) {
            converters.register(from, to, converter);
            return this;
        }

        public Builder withGlue(Class<?> type, Glue glue, int priority) {
            glues.register(type, glue, priority);
            return this;
        }

        public Builder withReaderSource(ReaderSource readerSource) {
            this.readerSource = readerSource;
            return this;
        }

        public Piper build() {
            if (readerSource == null) throw new RuntimeException("readerSource not configured");
            return new Piper(mappers, converters, glues, readerSource);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderWithDefaults() {
        return new Builder()
            .withMapper(String.class, "uppercase", (v, args) -> v.toUpperCase())
            .withMapper(LocalDate.class, "format", (v, args) -> {
                String pattern = (String) args[0];
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return v.format(formatter);
            })
            .withConverter(Integer.class, Number.class, v -> v)
            .withConverter(Double.class, Number.class, v -> v)
            .withGlue(Object.class, new BeanGlue(), 0)
            .withGlue(List.class, new ListGlue(), 1)
            .withGlue(Map.class, new MapGlue(), 2);
    }

    private final ExpressionParser expressionParser = new ExpressionParser();
    private final MapperRegistry mappers;
    private final ConverterRegistry converters;
    private final GlueRegistry glues;
    private final ReaderSource readerSource;
    private final TemplateParser templateParser;

    public Piper(MapperRegistry mappers, ConverterRegistry converters, GlueRegistry glues, ReaderSource readerSource) {
        this.templateParser = new TemplateParser(expressionParser, readerSource);
        this.mappers = mappers;
        this.converters = converters;
        this.glues = glues;
        this.readerSource = readerSource;
    }

    public EvalContext createEvalContext(Map<String, ?> vars) {
        DefaultEvalContext context = new DefaultEvalContext(mappers, converters, glues);
        vars.forEach(context::set);
        return context;
    }

    public Template loadTemplate(String path) throws IOException {
        try (Reader reader = readerSource.get(path)) {
            return templateParser.parse(new TemplateLexer(reader));
        }
    }
}