package tech.harmonysoft.oss.jenome.match.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;

@SuppressWarnings({"UnusedDeclaration"})
public class ClassComplianceMatcherTest {

    private ClassComplianceMatcher matcher;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        matcher = new ClassComplianceMatcher();
    }

    @Test
    public void toClass() {
        assertTrue(matcher.match(Number.class, Integer.class));
        assertTrue(matcher.match(Number.class, Number.class));
        assertFalse(matcher.match(Number.class, Integer.class, true));
        assertFalse(matcher.match(Integer.class, Number.class));
        assertFalse(matcher.match(String.class, Number.class));
    }

    @Test
    public void toParameterizedInterfaceType() {
        class TestClass implements Comparable<String> {
            @Override
            public int compareTo(String o) {
                return 0;
            }
        }
        Type type = TestClass.class.getGenericInterfaces()[0];
        assertTrue(matcher.match(Comparable.class, type));
        assertFalse(matcher.match(Serializable.class, type));
    }

    @Test
    public void toUpperBound() throws NoSuchFieldException {
        class Test {
            public Comparator<? extends Number> field;
        }

        Type intType = ((ParameterizedType)Test.class.getField("field").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(Integer.class, intType));
        assertTrue(matcher.match(Number.class, intType));
        assertFalse(matcher.match(Long.class, intType));
        assertFalse(matcher.match(String.class, intType));
        assertFalse(matcher.match(Object.class, intType));
    }

    @Test
    public void toLowerBound() throws NoSuchFieldException {
        class Test {
            public Comparator<? super Integer> field1;
            public Comparator<? super String> field2;
        }

        Type intType = ((ParameterizedType)Test.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(Integer.class, intType));
        assertFalse(matcher.match(Number.class, intType));
        assertTrue(matcher.match(Object.class, intType));
        assertFalse(matcher.match(Long.class, intType));

        Type stringType = ((ParameterizedType)Test.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(Integer.class, stringType));
        assertFalse(matcher.match(Number.class, stringType));
        assertFalse(matcher.match(String.class, stringType));
        assertTrue(matcher.match(Object.class, stringType));
    }

    @Test
    public void toBoundTypeVariable() {
        class SingleBoundClass<T extends Number> {}
        class MultipleBoundClass<T extends Number & CharSequence> {}

        Type numberType = SingleBoundClass.class.getTypeParameters()[0];
        assertFalse(matcher.match(Integer.class, numberType));
        assertTrue(matcher.match(Number.class, numberType));
        assertFalse(matcher.match(String.class, numberType));

        Type compoundType = MultipleBoundClass.class.getTypeParameters()[0];
        assertFalse(matcher.match(Integer.class, compoundType));
        assertFalse(matcher.match(Number.class, compoundType));
        assertFalse(matcher.match(String.class, compoundType));
    }

    @Test
    public void toUnboundTypeVariable() {
        class TestClass<T> {}
        Type type = TestClass.class.getTypeParameters()[0];
        assertFalse(matcher.match(Integer.class, type));
        assertFalse(matcher.match(Number.class, type));
        assertFalse(matcher.match(String.class, type));
    }

    @Test
    public void toUnboundGenericArray() {
        class TestClass<T> implements Comparable<T[]> {
            @Override
            public int compareTo(T[] o) {
                return 0;
            }
        }
        Type type = ((ParameterizedType)TestClass.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];

        assertFalse(matcher.match(Integer.class, type));
        assertFalse(matcher.match(Number.class, type));
        assertFalse(matcher.match(String.class, type));
        assertFalse(matcher.match(Integer[].class, type));
        assertFalse(matcher.match(Number[].class, type));
        assertFalse(matcher.match(String[].class, type));
    }

    @Test
    public void toBoundGenericArray() {
        class TestClass implements Comparable<Number[]> {
            @Override
            public int compareTo(Number[] o) {
                return 0;
            }
        }
        Type type = ((ParameterizedType)TestClass.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        assertFalse(matcher.match(Integer.class, type));
        assertFalse(matcher.match(Number.class, type));
        assertFalse(matcher.match(Integer[].class, type));
        assertTrue(matcher.match(Number[].class, type));
    }
}