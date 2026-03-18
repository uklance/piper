package com.example.template;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateLexerTest {
    @Test
    public void testNext() {
        // given
        String template = "<#if bean.foo>A<#else>B</#if><#list bean.list as item>${item}</#list>";

        // when
        TemplateLexer lexer = new TemplateLexer(template);
        List<TemplateToken> tokens = new ArrayList<>();
        tokens.add(lexer.next());
        while (tokens.getLast().type != TokenType.EOF) {
            tokens.add(lexer.next());
        }

        // then
        assertThat(tokens)
                .extracting(t -> String.format("%s[%s]", t.type, t.text))
                .containsExactly(
                        "DIRECTIVE_START[if bean.foo]",
                        "TEXT[A]",
                        "DIRECTIVE_START[else]",
                        "TEXT[B]",
                        "DIRECTIVE_END[if]",
                        "DIRECTIVE_START[list bean.list as item]",
                        "INTERPOLATION[item]",
                        "DIRECTIVE_END[list]",
                        "EOF[]"
                );
    }

}