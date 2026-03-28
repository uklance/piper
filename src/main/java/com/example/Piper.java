package com.example;

import com.example.converter.ConverterRegistry;
import com.example.directive.DirectiveParserRegistry;
import com.example.expression.DefaultEvalContext;
import com.example.expression.DefaultExpressionParser;
import com.example.expression.EvalContext;
import com.example.expression.ExpressionParser;
import com.example.glue.GlueRegistry;
import com.example.loader.TemplateLoader;
import com.example.mapper.MapperRegistry;
import com.example.operation.BinaryOperationRegistry;
import com.example.template.DefaultTemplateParser;
import com.example.template.Template;
import com.example.template.TemplateParser;

import java.io.IOException;
import java.util.Map;

public class Piper {
    private final MapperRegistry mappers;
    private final ConverterRegistry converters;
    private final GlueRegistry glues;
    private final TemplateLoader templateLoader;
    private final TemplateParser templateParser;
    private final BinaryOperationRegistry binaryOpsRegistry;

    public Piper(
            ExpressionParser expressionParser,
            TemplateLoader templateLoader,
            MapperRegistry mappers,
            ConverterRegistry converters,
            GlueRegistry glues,
            BinaryOperationRegistry binaryOpsRegistry,
            DirectiveParserRegistry directives
    ) {
        this.templateParser = new DefaultTemplateParser(expressionParser, templateLoader, this::createEvalContext, directives);
        this.mappers = mappers;
        this.converters = converters;
        this.glues = glues;
        this.templateLoader = templateLoader;
        this.binaryOpsRegistry = binaryOpsRegistry;
    }

    public static PiperBuilder builder() {
        return new PiperBuilder();
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