package com.example;

import com.example.converter.Converter;
import com.example.converter.ConverterRegistry;
import com.example.converter.DefaultConverterRegistry;
import com.example.directive.*;
import com.example.expression.DefaultEvalContext;
import com.example.expression.EvalContext;
import com.example.expression.ExpressionParser;
import com.example.glue.*;
import com.example.loader.ClasspathTemplateLoader;
import com.example.loader.TemplateLoader;
import com.example.mapper.*;
import com.example.operation.*;
import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Piper {
    public static class Builder {
        private final AtomicReference<Locale> locale = new AtomicReference<>(null);
        private final AtomicReference<DecimalFormatSymbols> decimalFormatSymbols = new AtomicReference<>(null);
        private final List<Consumer<DefaultConverterRegistry>> converterConfig = new ArrayList<>();
        private final List<Consumer<DefaultGlueRegistry>> glueConfig = new ArrayList<>();
        private final List<Consumer<DefaultBinaryOperationsRegistry>> binaryOpsConfig = new ArrayList<>();
        private final List<Consumer<DefaultMapperRegistry>> mapperConfig = new ArrayList<>();
        private final List<Consumer<DefaultDirectiveParserRegistry>> directiveConfig = new ArrayList<>();
        private TemplateLoader templateLoader;

        public <T> Builder withMapper(Class<T> type, String name, Mapper<T> mapper) {
            mapperConfig.add(registry -> registry.register(type, name, mapper));
            return this;
        }

        public <T> Builder withMapper(Class<T> type, String name, Supplier<Mapper<T>> mapperSupplier) {
            mapperConfig.add(mappers -> mappers.register(type, name, mapperSupplier.get()));
            return this;
        }

        public <F, T> Builder withConverter(Class<F> from, Class<T> to, Converter<F, T> converter) {
            converterConfig.add(registry -> registry.register(from, to, converter));
            return this;
        }

        public Builder withGlue(Class<?> type, Glue glue, int priority) {
            glueConfig.add(registry -> registry.register(type, glue, priority));
            return this;
        }

        public Builder withTemplateLoader(TemplateLoader templateLoader) {
            this.templateLoader = templateLoader;
            return this;
        }

        public <T> Builder withBinaryOperations(Class<T> type, BinaryOperations<T> binaryOps) {
            binaryOpsConfig.add(registry -> registry.register(type, binaryOps));
            return this;
        }

        public Builder withLocale(Locale locale) {
            this.locale.set(locale);
            return this;
        }

        public Builder withDecimalFormatSymbols(DecimalFormatSymbols symbols) {
            this.decimalFormatSymbols.set(symbols);
            return this;
        }

        public Builder withDirectiveParser(DirectiveParser parser) {
            directiveConfig.add(registry -> registry.register(parser));
            return this;
        }

        public Builder withDefaults() {
            return new Builder()
                .withLocale(Locale.getDefault())
                .withDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.getDefault()))
                .withTemplateLoader(new ClasspathTemplateLoader(Thread.currentThread().getContextClassLoader()))
                .withMapper(String.class, "uppercase", (v, args) -> v.toUpperCase())
                .withMapper(TemporalAccessor.class, "format", () -> new DateTimeFormatMapper(locale.get()))
                .withMapper(Number.class, "format", () -> new DecimalFormatMapper(decimalFormatSymbols.get()))
                .withConverter(Integer.class, Number.class, v -> v)
                .withConverter(Double.class, Number.class, v -> v)
                .withGlue(Object.class, new BeanGlue(), 0)
                .withGlue(List.class, new ListGlue(), 1)
                .withGlue(Map.class, new MapGlue(), 2)
                .withBinaryOperations(String.class, new StringBinaryOperations())
                .withBinaryOperations(Integer.class, new IntegerBinaryOperations())
                .withBinaryOperations(Double.class, new DoubleBinaryOperations())
                .withDirectiveParser(new IfDirectiveParser())
                .withDirectiveParser(new IncludeDirectiveParser())
                .withDirectiveParser(new AssignDirectiveParser())
                .withDirectiveParser(new ListDirectiveParser());
        }

        public Piper build() {
            if (templateLoader == null) throw new RuntimeException("templateLoader not configured");
            DefaultMapperRegistry mappers = new DefaultMapperRegistry();
            DefaultConverterRegistry converters = new DefaultConverterRegistry();
            DefaultGlueRegistry glues = new DefaultGlueRegistry();
            DefaultBinaryOperationsRegistry binaryOps = new DefaultBinaryOperationsRegistry();
            DefaultDirectiveParserRegistry directives = new DefaultDirectiveParserRegistry();
            mapperConfig.forEach(config -> config.accept(mappers));
            converterConfig.forEach(config -> config.accept(converters));
            glueConfig.forEach(config -> config.accept(glues));
            binaryOpsConfig.forEach(config -> config.accept(binaryOps));
            directiveConfig.forEach(config -> config.accept(directives));
            return new Piper(templateLoader, mappers, converters, glues, binaryOps, directives);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ExpressionParser expressionParser = new ExpressionParser();
    private final MapperRegistry mappers;
    private final ConverterRegistry converters;
    private final GlueRegistry glues;
    private final TemplateLoader templateLoader;
    private final TemplateParser templateParser;
    private final BinaryOperationRegistry binaryOpsRegistry;

    public Piper(TemplateLoader templateLoader, MapperRegistry mappers, ConverterRegistry converters, GlueRegistry glues, BinaryOperationRegistry binaryOpsRegistry, DirectiveParserRegistry directives) {
        this.templateParser = new TemplateParser(expressionParser, templateLoader, this::createEvalContext, directives);
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