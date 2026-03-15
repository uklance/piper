package com.example.expression;

import com.example.glue.BeanGlue;
import com.example.glue.DefaultGlueRegistry;
import com.example.glue.ListGlue;
import com.example.glue.MapGlue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
            String pattern = (String) args[0];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return v.format(formatter);
        });
        DefaultConverterRegistry converters=new DefaultConverterRegistry();

        converters.register(Integer.class,Number.class,v->v);
        converters.register(Double.class,Number.class,v->v);

        DefaultGlueRegistry glueRegistry = new DefaultGlueRegistry();
        glueRegistry.register(Object.class, new BeanGlue(), 0);
        glueRegistry.register(List.class, new ListGlue(), 1);
        glueRegistry.register(Map.class, new MapGlue(), 2);

        context = new DefaultExpressionContext(mappers,converters, glueRegistry);
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
    void testExpression() throws Exception {
        assertThat(eval("bean.number * 4 + bean.number2")).isEqualTo(23.0);
        assertThat(eval( "bean.name | uppercase")).isEqualTo("JOHN");
        assertThat(eval( "bean.flag1 ? 'Y' : 'N'")).isEqualTo("Y");
        assertThat(eval( "bean.flag2 ? 'Y' : 'N'")).isEqualTo("N");
        assertThat(eval( "bean.list[1]")).isEqualTo("B");
        assertThat(eval( "bean.localDate | format('d/M/yyyy')")).isEqualTo("3/12/2007");
        assertThat(eval( "bean.localDate | format('yyyy-MM-dd')")).isEqualTo("2007-12-03");
    }

    Object eval(String expression) throws Exception {
        return parser.parse(expression).eval(context);
    }
}
