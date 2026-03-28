package com.example.expression;

import com.example.converter.DefaultConverterRegistry;
import com.example.glue.*;
import com.example.mapper.DateTimeFormatMapper;
import com.example.mapper.DecimalFormatMapper;
import com.example.mapper.DefaultMapperRegistry;
import com.example.operation.DefaultBinaryOperationsRegistry;
import com.example.operation.DoubleBinaryOperations;
import com.example.operation.IntegerBinaryOperations;
import com.example.operation.StringBinaryOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionParserTest {
    private EvalContext context;
    private final ExpressionParser parser = new ExpressionParser();

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
        mappers.register(TemporalAccessor.class, "format", new DateTimeFormatMapper(Locale.UK));
        mappers.register(Number.class, "format", new DecimalFormatMapper(DecimalFormatSymbols.getInstance(Locale.UK)));

        DefaultConverterRegistry converters = new DefaultConverterRegistry();
        converters.register(Integer.class, Number.class, v -> v);
        converters.register(Double.class, Number.class, v -> v);

        MemberAccess memberAccess = new DefaultMemberAccess();
        DefaultGlueRegistry glueRegistry = new DefaultGlueRegistry();
        glueRegistry.register(Object.class, new BeanGlue(memberAccess), 0);
        glueRegistry.register(List.class, new ListGlue(memberAccess), 1);
        glueRegistry.register(Map.class, new MapGlue(memberAccess), 2);

        DefaultBinaryOperationsRegistry binaryOpsRegistry = new DefaultBinaryOperationsRegistry();
        binaryOpsRegistry.register(String.class, new StringBinaryOperations());
        binaryOpsRegistry.register(Integer.class, new IntegerBinaryOperations());
        binaryOpsRegistry.register(Double.class, new DoubleBinaryOperations());

        context = new DefaultEvalContext(mappers, converters, glueRegistry, binaryOpsRegistry);
        context.setValue("bean", bean);
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
    void testExpression() throws Exception {
        assertThat(eval("bean.number * 4 + bean.number2")).isEqualTo(23);
        assertThat(eval("4.0 * 5.0")).isEqualTo(20D);
        assertThat(eval("bean.name | uppercase")).isEqualTo("JOHN");
        assertThat(eval("bean.flag1 ? 'Y' : 'N'")).isEqualTo("Y");
        assertThat(eval("bean.flag2 ? 'Y' : 'N'")).isEqualTo("N");
        assertThat(eval("bean.list[1]")).isEqualTo("B");
        assertThat(eval("bean.localDate | format('d/M/yyyy')")).isEqualTo("3/12/2007");
        assertThat(eval("bean.localDate | format('yyyy-MM-dd')")).isEqualTo("2007-12-03");
        assertThat(eval("12345.6789 | format('#,###.00')")).isEqualTo("12,345.68");
        assertThat(eval("4 * 3 + 10")).isEqualTo(22);
        assertThat(eval("10 + 4 * 3")).isEqualTo(22);
        assertThat(eval("'a' + 'b'")).isEqualTo("ab");
    }

    Object eval(String expression) throws Exception {
        return parser.parse(expression).eval(context);
    }
}
