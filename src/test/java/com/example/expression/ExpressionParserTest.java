package com.example.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionParserTest {
    private ExpressionContext context;
    private ExpressionParser parser = new ExpressionParser();

    @BeforeEach
    public void beforeEach() {
        Bean bean=new Bean();
        bean.name="John";
        bean.number=5;
        bean.number2=3;
        bean.flag1=true;
        bean.flag2=false;
        bean.list=List.of("A","B","C");
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
    void testExpression(){
        assertThat(eval("bean.number * 4 + bean.number2")).isEqualTo(23.0);
        assertThat(eval( "bean.name | uppercase")).isEqualTo("JOHN");
        assertThat(eval( "bean.flag1 ? 'Y' : 'N'")).isEqualTo("Y");
        assertThat(eval( "bean.flag2 ? 'Y' : 'N'")).isEqualTo("N");
        assertThat(eval( "bean.list[1]")).isEqualTo("B");
        assertThat(eval( "bean.localDate | format('d/M/yyyy')")).isEqualTo("3/12/2007");
        assertThat(eval( "bean.localDate | format('yyyy-MM-dd')")).isEqualTo("2007-12-03");
    }

    Object eval(String expression) {
        return parser.parse(expression).eval(context);
    }
}
