package com.example.template;

import com.example.expression.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserTest {
    private ExpressionContext context;
    private TemplateParser templateParser = new TemplateParser(new ExpressionParser());

    @BeforeEach
    public void beforeEach() {
        Bean bean=new Bean();
        bean.name="John";
        bean.number=5;
        bean.number2=3;
        bean.flag1=true;
        bean.flag2=false;
        bean.list= List.of("A","B","C");
        bean.localDate = LocalDate.parse("2007-12-03");

        DefaultMapperRegistry mappers=new DefaultMapperRegistry();
        mappers.register(String.class,"uppercase",(v,args)->v.toUpperCase());
        mappers.register(LocalDate.class,"format",(v, args)-> {
            String pattern = (String) args.getFirst();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return v.format(formatter);
        });
        DefaultConverterRegistry converters=new DefaultConverterRegistry();

        converters.register(Integer.class,Number.class,v->v);
        converters.register(Double.class,Number.class,v->v);

        context = new DefaultExpressionContext(mappers,converters);
        context.set("bean",bean);
    }

    static class Bean{
        public String name;
        public int number;
        public int number2;
        public boolean flag1;
        public boolean flag2;
        public List<Object> list;
        public LocalDate localDate;
    }

    @Test
    public void test() throws IOException {
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
    public void testNested() throws IOException {
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

}