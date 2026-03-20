package com.example.template;

import com.example.converter.DefaultConverterRegistry;
import com.example.expression.DefaultEvalContext;
import com.example.expression.EvalContext;
import com.example.expression.ExpressionParser;
import com.example.glue.BeanGlue;
import com.example.glue.DefaultGlueRegistry;
import com.example.glue.ListGlue;
import com.example.glue.MapGlue;
import com.example.mapper.DefaultMapperRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserTest {
    private EvalContext context;
    private TemplateParser templateParser;

    @BeforeEach
    public void beforeEach() {
        Bean bean = new Bean();
        bean.name = "John";
        bean.number = 5;
        bean.number2 = 3;
        bean.flag1 = true;
        bean.flag2 = false;
        bean.list = List.of("A", "B", "C");
        bean.localDate = LocalDate.parse("2007-12-03");

        DefaultMapperRegistry mappers = new DefaultMapperRegistry();
        mappers.register(String.class, "uppercase", (v, args) -> v.toUpperCase());
        mappers.register(LocalDate.class, "format", (v, args) -> {
            String pattern = (String) args[0];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return v.format(formatter);
        });
        DefaultConverterRegistry converters = new DefaultConverterRegistry();

        converters.register(Integer.class, Number.class, v -> v);
        converters.register(Double.class, Number.class, v -> v);

        DefaultGlueRegistry glueRegistry = new DefaultGlueRegistry();
        glueRegistry.register(Object.class, new BeanGlue(), 0);
        glueRegistry.register(List.class, new ListGlue(), 1);
        glueRegistry.register(Map.class, new MapGlue(), 2);

        Map<String, String> includeMap = Map.of("hello-current.ftl", "Hello ${current}");
        ReaderSource readerSource = path -> {
            if (!includeMap.containsKey(path)) throw new IOException("No such resource " + path);
            String ftl = includeMap.get(path);
            return new StringReader(ftl);
        };

        templateParser = new TemplateParser(new ExpressionParser(), readerSource);
        context = new DefaultEvalContext(mappers, converters, glueRegistry);
        context.set("bean", bean);
    }

    static class Bean {
        public String name;
        public int number;
        public int number2;
        public boolean flag1;
        public boolean flag2;
        public List<Object> list;
        public LocalDate localDate;

        public String getName() {
            return name;
        }

        public int getNumber() {
            return number;
        }

        public int getNumber2() {
            return number2;
        }

        public boolean isFlag1() {
            return flag1;
        }

        public boolean isFlag2() {
            return flag2;
        }

        public List<Object> getList() {
            return list;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }
    }

    @Test
    public void test() throws Exception {
        Template template = templateParser.parse("""
                flag1: <#if bean.flag1>Y<#else>N</#if>
                flag2: <#if bean.flag2>Y<#else>N</#if>
                list: <#list bean.list as entry>[${entry}],</#list>
                """
        );

        String result = template.apply(context);
        assertThat(result)
                .contains("flag1: Y")
                .contains("flag2: N")
                .contains("list: [A],[B],[C],");
    }

    @Test
    public void testNested() throws Exception {
        Template template = templateParser.parse("""
                <#list bean.list as i>
                    <#list bean.list as j>
                        <#list bean.list as k>${i}${j}${k},</#list>
                    </#list>
                </#list>                        
                """
        );

        String result = template.apply(context);
        assertThat(result)
                .contains("AAA,AAB,AAC")
                .contains("ABA,ABB,ABC")
                .contains("ACA,ACB,ACC")
                .contains("BAA,BAB,BAC")
                .contains("BBA,BBB,BBC")
                .contains("BCA,BCB,BCC")
                .contains("CAA,CAB,CAC")
                .contains("CBA,CBB,CBC")
                .contains("CCA,CCB,CCC");
    }

    @Test
    public void testAssign() throws Exception {
        Template template = templateParser.parse("""
                <#assign foo='abc'>
                foo is ${foo}
                """
        );

        String result = template.apply(context);
        assertThat(result.trim()).isEqualTo("foo is abc");
    }

    @Test
    public void testInclude() throws Exception {
        Template template = templateParser.parse("<#list bean.list as current><#include 'hello-current.ftl'> </#list>");

        String result = template.apply(context);
        assertThat(result.trim()).isEqualTo("Hello A Hello B Hello C");
    }
}