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
import com.example.template.DefaultTemplateParser;
import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Piper {
    public interface Settings {
        Locale getLocale();
        DecimalFormatSymbols getDecimalFormatSymbols();
        MemberAccess getMemberAccess();
    }

    private static class SettingsBuilder {
        private Locale locale;
        private DecimalFormatSymbols decimalFormatSymbols;
        private MemberAccess memberAccess;

        public SettingsBuilder withLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public SettingsBuilder withDecimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
            this.decimalFormatSymbols = decimalFormatSymbols;
            return this;
        }

        public SettingsBuilder withMemberAccess(MemberAccess memberAccess) {
            this.memberAccess = memberAccess;
            return this;
        }

        public Settings build() {
            // take a copy so the builder can be mutated and used again
            Locale _locale = this.locale;
            DecimalFormatSymbols _decimalFormatSymbols = this.decimalFormatSymbols;
            MemberAccess _memberAccess = this.memberAccess;

            return new Settings() {
                @Override
                public Locale getLocale() {
                    return _locale;
                }

                @Override
                public DecimalFormatSymbols getDecimalFormatSymbols() {
                    return _decimalFormatSymbols;
                }

                @Override
                public MemberAccess getMemberAccess() {
                    return _memberAccess;
                }
            };
        }
    }
    public static class Builder {
        private final SettingsBuilder settingsBuilder = new SettingsBuilder();
        private final List<BiConsumer<DefaultConverterRegistry, Settings>> converterConfig = new ArrayList<>();
        private final List<BiConsumer<DefaultGlueRegistry, Settings>> glueConfig = new ArrayList<>();
        private final List<BiConsumer<DefaultBinaryOperationsRegistry, Settings>> binaryOpsConfig = new ArrayList<>();
        private final List<BiConsumer<DefaultMapperRegistry, Settings>> mapperConfig = new ArrayList<>();
        private final List<BiConsumer<DefaultDirectiveParserRegistry, Settings>> directiveConfig = new ArrayList<>();
        private TemplateLoader templateLoader;

        public <T> Builder withMapper(Class<T> type, String name, Mapper<T> mapper) {
            mapperConfig.add((registry, settings) -> registry.register(type, name, mapper));
            return this;
        }

        public <T> Builder withMapper(Class<T> type, String name, Function<Settings, Mapper<T>> mapperSupplier) {
            mapperConfig.add((registry, settings) -> registry.register(type, name, mapperSupplier.apply(settings)));
            return this;
        }

        public <F, T> Builder withConverter(Class<F> from, Class<T> to, Converter<F, T> converter) {
            converterConfig.add((registry, settings) -> registry.register(from, to, converter));
            return this;
        }

        public Builder withGlue(Class<?> type, Glue glue, int priority) {
            glueConfig.add((registry, settings) -> registry.register(type, glue, priority));
            return this;
        }

        public Builder withGlue(Class<?> type, Function<Settings, Glue> glueSupplier, int priority) {
            glueConfig.add((registry, settings) -> registry.register(type, glueSupplier.apply(settings), priority));
            return this;
        }

        public Builder withTemplateLoader(TemplateLoader templateLoader) {
            this.templateLoader = templateLoader;
            return this;
        }

        public <T> Builder withBinaryOperations(Class<T> type, BinaryOperations<T> binaryOps) {
            binaryOpsConfig.add((registry, settings) -> registry.register(type, binaryOps));
            return this;
        }

        public Builder withLocale(Locale locale) {
            settingsBuilder.withLocale(locale);
            return this;
        }

        public Builder withDecimalFormatSymbols(DecimalFormatSymbols symbols) {
            settingsBuilder.withDecimalFormatSymbols(symbols);
            return this;
        }

        public Builder withMemberAccess(MemberAccess memberAccess) {
            settingsBuilder.withMemberAccess(memberAccess);
            return this;
        }

        public Builder withDirectiveParser(DirectiveParser parser) {
            directiveConfig.add((registry, settings) -> registry.register(parser));
            return this;
        }

        public Builder withDefaults() {
            return new Builder()
                .withLocale(Locale.getDefault())
                .withDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.getDefault()))
                .withMemberAccess(new DefaultMemberAccess())
                .withTemplateLoader(new ClasspathTemplateLoader(Thread.currentThread().getContextClassLoader()))
                .withMapper(String.class, "uppercase", (v, args) -> v.toUpperCase())
                .withMapper(TemporalAccessor.class, "format", settings -> new DateTimeFormatMapper(settings.getLocale()))
                .withMapper(Number.class, "format", settings -> new DecimalFormatMapper(settings.getDecimalFormatSymbols()))
                .withConverter(Integer.class, Number.class, v -> v)
                .withConverter(Double.class, Number.class, v -> v)
                .withGlue(Object.class, settings -> new BeanGlue(settings.getMemberAccess()), 0)
                .withGlue(List.class, settings -> new ListGlue(settings.getMemberAccess()), 1)
                .withGlue(Map.class, settings -> new MapGlue(settings.getMemberAccess()), 2)
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
            Settings settings = settingsBuilder.build();
            DefaultMapperRegistry mappers = new DefaultMapperRegistry();
            DefaultConverterRegistry converters = new DefaultConverterRegistry();
            DefaultGlueRegistry glues = new DefaultGlueRegistry();
            DefaultBinaryOperationsRegistry binaryOps = new DefaultBinaryOperationsRegistry();
            DefaultDirectiveParserRegistry directives = new DefaultDirectiveParserRegistry();
            mapperConfig.forEach(config -> config.accept(mappers, settings));
            converterConfig.forEach(config -> config.accept(converters, settings));
            glueConfig.forEach(config -> config.accept(glues, settings));
            binaryOpsConfig.forEach(config -> config.accept(binaryOps, settings));
            directiveConfig.forEach(config -> config.accept(directives, settings));
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
        this.templateParser = new DefaultTemplateParser(expressionParser, templateLoader, this::createEvalContext, directives);
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