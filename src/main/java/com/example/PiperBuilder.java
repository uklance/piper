package com.example;

import com.example.converter.Converter;
import com.example.converter.DefaultConverterRegistry;
import com.example.directive.*;
import com.example.expression.DefaultExpressionParser;
import com.example.expression.ExpressionParser;
import com.example.glue.*;
import com.example.loader.ClasspathTemplateLoader;
import com.example.loader.TemplateLoader;
import com.example.mapper.DateTimeFormatMapper;
import com.example.mapper.DecimalFormatMapper;
import com.example.mapper.DefaultMapperRegistry;
import com.example.mapper.Mapper;
import com.example.operation.*;

import java.text.DecimalFormatSymbols;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PiperBuilder {
    private final List<Consumer<DefaultSettings>> settingsConfig = new ArrayList<>();
    private final List<BiConsumer<DefaultConverterRegistry, Settings>> converterConfig = new ArrayList<>();
    private final List<BiConsumer<DefaultGlueRegistry, Settings>> glueConfig = new ArrayList<>();
    private final List<BiConsumer<DefaultBinaryOperationsRegistry, Settings>> binaryOpsConfig = new ArrayList<>();
    private final List<BiConsumer<DefaultMapperRegistry, Settings>> mapperConfig = new ArrayList<>();
    private final List<BiConsumer<DefaultDirectiveParserRegistry, Settings>> directiveConfig = new ArrayList<>();
    private TemplateLoader templateLoader;

    public <T> PiperBuilder withMapper(Class<T> type, String name, Mapper<T> mapper) {
        mapperConfig.add((registry, settings) -> registry.register(type, name, mapper));
        return this;
    }

    public <T> PiperBuilder withMapper(Class<T> type, String name, Function<Settings, Mapper<T>> mapperSupplier) {
        mapperConfig.add((registry, settings) -> registry.register(type, name, mapperSupplier.apply(settings)));
        return this;
    }

    public <F, T> PiperBuilder withConverter(Class<F> from, Class<T> to, Converter<F, T> converter) {
        converterConfig.add((registry, settings) -> registry.register(from, to, converter));
        return this;
    }

    public PiperBuilder withGlue(Class<?> type, int priority, Glue<?> glue) {
        glueConfig.add((registry, settings) -> registry.register(type, priority, glue));
        return this;
    }

    public PiperBuilder withGlue(Class<?> type, int priority, Function<Settings, Glue<?>> glueSupplier) {
        glueConfig.add((registry, settings) -> registry.register(type, priority, glueSupplier.apply(settings)));
        return this;
    }

    public PiperBuilder withTemplateLoader(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
        return this;
    }

    public <T> PiperBuilder withBinaryOperations(Class<T> type, BinaryOperations<T> binaryOps) {
        binaryOpsConfig.add((registry, settings) -> registry.register(type, binaryOps));
        return this;
    }

    public PiperBuilder withLocale(Locale locale) {
        settingsConfig.add(settings -> settings.setLocale(locale));
        return this;
    }

    public PiperBuilder withDecimalFormatSymbols(DecimalFormatSymbols symbols) {
        settingsConfig.add(settings -> settings.setDecimalFormatSymbols(symbols));
        return this;
    }

    public PiperBuilder withMemberAccess(MemberAccess memberAccess) {
        settingsConfig.add(settings -> settings.setMemberAccess(memberAccess));
        return this;
    }

    public PiperBuilder withDirectiveParser(DirectiveParser parser) {
        directiveConfig.add((registry, settings) -> registry.register(parser));
        return this;
    }

    public PiperBuilder withDefaults() {
        return new PiperBuilder()
                .withLocale(Locale.getDefault())
                .withDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.getDefault()))
                .withMemberAccess(new DefaultMemberAccess())
                .withTemplateLoader(new ClasspathTemplateLoader(Thread.currentThread().getContextClassLoader()))
                .withMapper(String.class, "uppercase", (v, args) -> v.toUpperCase())
                .withMapper(TemporalAccessor.class, "format", settings -> new DateTimeFormatMapper(settings.getLocale()))
                .withMapper(Number.class, "format", settings -> new DecimalFormatMapper(settings.getDecimalFormatSymbols()))
                .withConverter(Integer.class, Number.class, v -> v)
                .withConverter(Double.class, Number.class, v -> v)
                .withGlue(Object.class, 0, settings -> new BeanGlue(settings.getMemberAccess()))
                .withGlue(List.class, 1, settings -> new ListGlue(settings.getMemberAccess()))
                .withGlue(Map.class, 2, settings -> new MapGlue(settings.getMemberAccess()))
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
        DefaultSettings settings = new DefaultSettings();
        settingsConfig.forEach(config -> config.accept(settings));
        DefaultMapperRegistry mappers = new DefaultMapperRegistry(settings.getMemberAccess());
        DefaultConverterRegistry converters = new DefaultConverterRegistry(settings.getMemberAccess());
        DefaultGlueRegistry glues = new DefaultGlueRegistry();
        DefaultBinaryOperationsRegistry binaryOps = new DefaultBinaryOperationsRegistry();
        DefaultDirectiveParserRegistry directives = new DefaultDirectiveParserRegistry();
        mapperConfig.forEach(config -> config.accept(mappers, settings));
        converterConfig.forEach(config -> config.accept(converters, settings));
        glueConfig.forEach(config -> config.accept(glues, settings));
        binaryOpsConfig.forEach(config -> config.accept(binaryOps, settings));
        directiveConfig.forEach(config -> config.accept(directives, settings));
        ExpressionParser expressionParser = new DefaultExpressionParser();
        return new Piper(expressionParser, templateLoader, mappers, converters, glues, binaryOps, directives);
    }
}
