package com.example.template;

import com.example.Piper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserTest {
    private Piper piper;
    private Map<String, String> templateMap;

    @BeforeEach
    public void beforeEach() {
        templateMap = new HashMap<>();

        ReaderSource readerSource = path -> {
            if (!templateMap.containsKey(path)) throw new IOException("No such resource " + path);
            String ftl = templateMap.get(path);
            return new StringReader(ftl);
        };
        piper = Piper.builderWithDefaults().withReaderSource(readerSource).build();
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
        templateMap.put("test.ftl", """
                flag1: <#if bean.flag1>Y<#else>N</#if>
                flag2: <#if bean.flag2>Y<#else>N</#if>
                list: <#list bean.list as entry>[${entry}],</#list>"""
        );
        Bean bean = new Bean();
        bean.flag1 = true;
        bean.flag2 = false;
        bean.list = List.of("A", "B", "C");

        Template template = piper.loadTemplate("test.ftl");

        String result = template.apply(piper.createEvalContext(Map.of("bean", bean)));
        assertThat(result)
                .contains("flag1: Y")
                .contains("flag2: N")
                .contains("list: [A],[B],[C],");
    }

    @Test
    public void testNested() throws Exception {
        templateMap.put("testNested.ftl","""
                <#list bean.list as i>
                    <#list bean.list as j>
                        <#list bean.list as k>${i}${j}${k},</#list>
                    </#list>
                </#list>"""
        );
        Bean bean = new Bean();
        bean.list = List.of("A", "B", "C");

        Template template = piper.loadTemplate("testNested.ftl");
        String result = template.apply(piper.createEvalContext(Map.of("bean", bean)));
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
        templateMap.put("testAssign.ftl", """
            <#assign foo='abc'>
            foo is ${foo}"""
        );

        Template template = piper.loadTemplate("testAssign.ftl");
        String result = template.apply(piper.createEvalContext(Collections.emptyMap()));
        assertThat(result.trim()).isEqualTo("foo is abc");
    }

    @Test
    public void testInclude() throws Exception {
        templateMap.put("testInclude.ftl", """
            <#list bean.list as current>
               <#include 'greeting-x.ftl'.replaceFirst('x', current)>
            </#list>"""
        );
        templateMap.put("greeting-A.ftl", "Good morning ${current}");
        templateMap.put("greeting-B.ftl", "Hello ${current}");
        templateMap.put("greeting-C.ftl", "Good evening ${current}");

        Bean bean = new Bean();
        bean.list = List.of("A", "B", "C");

        Template template = piper.loadTemplate("testInclude.ftl");
        String result = template.apply(piper.createEvalContext(Map.of("bean", bean)));

        assertThat(result)
                .contains("Good morning A")
                .contains("Hello B")
                .contains("Good evening C");
    }

    @Test
    public void testElseIf() throws Exception {
        templateMap.put("testElseIf.ftl", """
                test1: <#if true>1<#elseif true>2<#else>3</#if>
                test2: <#if false>1<#elseif true>2<#else>3</#if>
                test3: <#if false>1<#elseif false>2<#else>3</#if>
                """
        );

        Template template = piper.loadTemplate("testElseIf.ftl");
        String result = template.apply(piper.createEvalContext(Collections.emptyMap()));
        assertThat(result)
                .contains("test1: 1")
                .contains("test2: 2")
                .contains("test3: 3");
    }
}