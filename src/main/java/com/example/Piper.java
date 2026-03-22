package com.example;

import com.example.converter.Converter;
import com.example.converter.ConverterRegistry;
import com.example.converter.DefaultConverterRegistry;
import com.example.expression.DefaultEvalContext;
import com.example.expression.EvalContext;
import com.example.expression.ExpressionParser;
import com.example.glue.*;
import com.example.loader.ClasspathTemplateLoader;
import com.example.loader.TemplateLoader;
import com.example.mapper.DefaultMapperRegistry;
import com.example.mapper.Mapper;
import com.example.mapper.MapperRegistry;
import com.example.operation.*;
import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Piper {
    public static class Builder {
        private final DefaultMapperRegistry mappers = new DefaultMapperRegistry();
        private final DefaultConverterRegistry converters = new DefaultConverterRegistry();
        private final DefaultGlueRegistry glues = new DefaultGlueRegistry();
        private final DefaultBinaryOperationsRegistry binaryOpsRegistry = new DefaultBinaryOperationsRegistry();
        private TemplateLoader templateLoader;

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

        public Builder withTemplateLoader(TemplateLoader templateLoader) {
            this.templateLoader = templateLoader;
            return this;
        }

        public <T> Builder withBinaryOperations(Class<T> type, BinaryOperations<T> binaryOps) {
            binaryOpsRegistry.register(type, binaryOps);
            return this;
        }

        public Piper build() {
            if (templateLoader == null) throw new RuntimeException("templateLoader not configured");
            return new Piper(mappers, converters, glues, templateLoader, binaryOpsRegistry);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderWithDefaults() {
        return new Builder()
            .withTemplateLoader(new ClasspathTemplateLoader(Thread.currentThread().getContextClassLoader()))
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
            .withGlue(Map.class, new MapGlue(), 2)
            .withBinaryOperations(String.class, new StringBinaryOperations())
            .withBinaryOperations(Integer.class, new IntegerBinaryOperations())
            .withBinaryOperations(Double.class, new DoubleBinaryOperations());
    }

    private final ExpressionParser expressionParser = new ExpressionParser();
    private final MapperRegistry mappers;
    private final ConverterRegistry converters;
    private final GlueRegistry glues;
    private final TemplateLoader templateLoader;
    private final TemplateParser templateParser;
    private final BinaryOperationRegistry binaryOpsRegistry;

    public Piper(MapperRegistry mappers, ConverterRegistry converters, GlueRegistry glues, TemplateLoader templateLoader, BinaryOperationRegistry binaryOpsRegistry) {
        this.templateParser = new TemplateParser(expressionParser, templateLoader, this::createEvalContext);
        this.mappers = mappers;
        this.converters = converters;
        this.glues = glues;
        this.templateLoader = templateLoader;
        this.binaryOpsRegistry = binaryOpsRegistry;
    }

    private EvalContext createEvalContext(Map<String, ?> vars) {
        DefaultEvalContext context = new DefaultEvalContext(mappers, converters, glues, binaryOpsRegistry);
        vars.forEach(context::setValue);
        return context;
    }

    public Template loadTemplate(String path) throws IOException {
        return templateLoader.load(path, templateParser);
    }
}