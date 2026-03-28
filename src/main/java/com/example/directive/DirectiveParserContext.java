package com.example.directive;

import com.example.expression.Expression;
import com.example.template.Template;
import com.example.template.TemplateLexer;
import com.example.template.TemplateNode;
import com.example.template.TemplateToken;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

public interface DirectiveParserContext {
    boolean isDirectiveStart(TemplateToken token, String name);
    boolean isDirectiveEnd(TemplateToken token, String name);
    Expression parseExpression(String expr);
    List<TemplateNode> parseNodes(TemplateLexer lexer, Predicate<TemplateToken> stopPredicate) throws IOException;
    Template loadTemplate(String path) throws IOException;
}
