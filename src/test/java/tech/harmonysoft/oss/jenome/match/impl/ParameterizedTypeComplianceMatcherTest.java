package tech.harmonysoft.oss.jenome.match.impl;

import java.lang.reflect.*;
import java.util.Comparator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Denis Zhdanov
 */
@SuppressWarnings({"UnusedDeclaration", "SuppressionAnnotation", "RawUseOfParameterizedType"})
public class ParameterizedTypeComplianceMatcherTest {

    private ParameterizedTypeComplianceMatcher matcher;

    @Before
    public void setUp() throws Exception {
        matcher = new ParameterizedTypeComplianceMatcher();
    }

    @Test
    public void toClass() {
        class TestClass implements Comparable<Integer> {
            @Override
            public int compareTo(Integer o) {
                return 0;
            }
        }
        class RawClass implements Comparable {
            @Override
            public int compareTo(Object o) {
                return 0;
            }
        }

        assertTrue(matcher.match((ParameterizedType) TestClass.class.getGenericInterfaces()[0], Integer.class));
        assertFalse(matcher.match((ParameterizedType) TestClass.class.getGenericInterfaces()[0], Long.class));
        assertFalse(matcher.match((ParameterizedType) TestClass.class.getGenericInterfaces()[0], StringBuilder.class));
        assertFalse(matcher.match((ParameterizedType) TestClass.class.getGenericInterfaces()[0], RawClass.class));
    }

    @Test
    public void toParameterizedType() {
        class SimpleBaseClass implements TestInterface<Integer, Long, String> {}
        class SimpleMatchedClass extends TestInterfaceImpl<Integer, Long, String> {}
        class SimpleUnmatchedClass extends TestInterfaceImpl<Integer, Long, Number> {}

        class ComplexBaseClass implements TestInterface<TestInterface<Integer, Long, Number>, String, Long> {}
        class ComplexMatchedClass extends TestInterfaceImpl<TestInterface<Integer, Long, Number>, String, Long> {}
        class ComplexUnmatchedClass1 extends TestInterfaceImpl<TestInterface<Integer, Long, String>, String, Long> {}
        class ComplexUnmatchedClass2 extends TestInterfaceImpl<TestInterface<Integer, Long, Long>, String, Long> {}
        class ComplexUnmatchedClass3 extends TestInterfaceImpl<TestInterface<Integer, Long, Integer>, String, Long> {}

        ParameterizedType simpleBaseType = (ParameterizedType) SimpleBaseClass.class.getGenericInterfaces()[0];
        assertTrue(matcher.match(simpleBaseType, SimpleMatchedClass.class.getGenericSuperclass()));
        assertFalse(matcher.match(simpleBaseType, SimpleUnmatchedClass.class.getGenericSuperclass()));

        ParameterizedType complexBaseType = (ParameterizedType) ComplexBaseClass.class.getGenericInterfaces()[0];
        assertTrue(matcher.match(complexBaseType, ComplexMatchedClass.class.getGenericSuperclass()));
        assertFalse(matcher.match(complexBaseType, ComplexUnmatchedClass1.class.getGenericSuperclass()));
        assertFalse(matcher.match(complexBaseType, ComplexUnmatchedClass2.class.getGenericSuperclass()));
        assertFalse(matcher.match(complexBaseType, ComplexUnmatchedClass3.class.getGenericSuperclass()));
    }

    @Test
    public void toParameterizedTypeWithUnresolvedTypeVariables() {
        class TestClass1<T> implements TestInterface<T, T, T> {}
        class TestClass2<A> implements TestInterface<A, A, A> {}
        class TestClass3<A, B, C> implements TestInterface<A, B, C> {}

        assertTrue(matcher.match((ParameterizedType)TestClass1.class.getGenericInterfaces()[0], TestClass2.class));
        assertFalse(matcher.match((ParameterizedType)TestClass1.class.getGenericInterfaces()[0], TestClass3.class));
    }

    @Test
    public void toParameterizedTypeWithGenericArrays() {
        class BaseClassWithoutBounds implements TestInterface<TestInterface<Integer, Long, String>[], String, Long> {}
        class BaseClassWithUpperBounds
                implements TestInterface<TestInterface<Integer, Long, ? extends Number>[], String, Long> {}
        class BaseClassWithLowerBounds
                implements TestInterface<TestInterface<Integer, Long, ? super Number>[], String, Long> {}

        class TestClass1 extends TestInterfaceImpl<TestInterface<Integer, Long, String>[], String, Long> {}
        class TestClass2 extends TestInterfaceImpl<TestInterface<Integer, Long, Number>[], String, Long> {}
        class TestClass3 extends TestInterfaceImpl<TestInterface<Integer, Long, Long>[], String, Long> {}
        class TestClass4 extends TestInterfaceImpl<TestInterface<Integer, Long, Object>[], String, Long> {}

        ParameterizedType baseTypeWithoutBounds
                = (ParameterizedType)BaseClassWithoutBounds.class.getGenericInterfaces()[0];
        assertTrue(matcher.match(baseTypeWithoutBounds, TestClass1.class.getGenericSuperclass()));
        assertFalse(matcher.match(baseTypeWithoutBounds, TestClass2.class.getGenericSuperclass()));
        assertFalse(matcher.match(baseTypeWithoutBounds, TestClass3.class.getGenericSuperclass()));
        assertFalse(matcher.match(baseTypeWithoutBounds, TestClass4.class.getGenericSuperclass()));

        ParameterizedType baseTypeWithUpperBounds
                = (ParameterizedType)BaseClassWithUpperBounds.class.getGenericInterfaces()[0];
        assertFalse(matcher.match(baseTypeWithUpperBounds, TestClass1.class.getGenericSuperclass()));
        assertTrue(matcher.match(baseTypeWithUpperBounds, TestClass2.class.getGenericSuperclass()));
        assertTrue(matcher.match(baseTypeWithUpperBounds, TestClass3.class.getGenericSuperclass()));
        assertFalse(matcher.match(baseTypeWithUpperBounds, TestClass4.class.getGenericSuperclass()));

        ParameterizedType baseTypeWithLowerBounds
                = (ParameterizedType)BaseClassWithLowerBounds.class.getGenericInterfaces()[0];
        assertFalse(matcher.match(baseTypeWithLowerBounds, TestClass1.class.getGenericSuperclass()));
        assertTrue(matcher.match(baseTypeWithLowerBounds, TestClass2.class.getGenericSuperclass()));
        assertFalse(matcher.match(baseTypeWithLowerBounds, TestClass3.class.getGenericSuperclass()));
        assertTrue(matcher.match(baseTypeWithLowerBounds, TestClass4.class.getGenericSuperclass()));
    }

    @Test
    public void toWildcard() throws NoSuchFieldException {
        class Base<T> {}
        class TestClass1<T extends Base<? extends Number>> {}
        class TestClass2<T extends Base<? super Number>> {}
        class TestClass3<T extends Base<Number>> {}
        class Test {
            public Comparator<? extends Base<CharSequence>> field1;
            public Comparator<? extends Base<Long>> field2;
            public Comparator<? extends Base<Object>> field3;
            public Comparator<? extends Base<Number>> field4;
        }

        Type candidateCharSequence
                = ((ParameterizedType)Test.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        Type candidateLong
                = ((ParameterizedType)Test.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        Type candidateObject
                = ((ParameterizedType)Test.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        Type candidateNumber
                = ((ParameterizedType)Test.class.getField("field4").getGenericType()).getActualTypeArguments()[0];

        ParameterizedType baseWithUpperBounds
                = (ParameterizedType) TestClass1.class.getTypeParameters()[0].getBounds()[0];
        assertTrue(matcher.match(baseWithUpperBounds, candidateLong));
        assertTrue(matcher.match(baseWithUpperBounds, candidateNumber));
        assertFalse(matcher.match(baseWithUpperBounds, candidateCharSequence));
        assertFalse(matcher.match(baseWithUpperBounds, candidateObject));

        ParameterizedType baseWithLowerBounds
                = (ParameterizedType) TestClass2.class.getTypeParameters()[0].getBounds()[0];
        assertTrue(matcher.match(baseWithLowerBounds, candidateNumber));
        assertTrue(matcher.match(baseWithLowerBounds, candidateObject));
        assertFalse(matcher.match(baseWithLowerBounds, candidateLong));
        assertFalse(matcher.match(baseWithLowerBounds, candidateCharSequence));

        ParameterizedType baseWithoutBounds
                = (ParameterizedType) TestClass3.class.getTypeParameters()[0].getBounds()[0];
        assertTrue(matcher.match(baseWithoutBounds, candidateNumber));
        assertFalse(matcher.match(baseWithoutBounds, candidateLong));
        assertFalse(matcher.match(baseWithoutBounds, candidateCharSequence));
        assertFalse(matcher.match(baseWithoutBounds, candidateObject));
    }

// TODO den fix
//    @Test
    public void toGenericArrayType() {
        class BaseClass implements TestInterface<Integer[], String, Long> {}
        class SubClass extends TestInterfaceImpl<Integer[], String, Long> {}

        ParameterizedType parameterizedBaseType = (ParameterizedType)BaseClass.class.getGenericInterfaces()[0];
        ParameterizedType parameterizedSubType = (ParameterizedType)SubClass.class.getGenericSuperclass();
        GenericArrayType genericArrayType = (GenericArrayType)parameterizedSubType.getActualTypeArguments()[0];
        assertFalse(matcher.match(parameterizedBaseType, genericArrayType));
    }

    interface TestInterface<A, B, C> {}
    private class TestInterfaceImpl<A, B, C> implements TestInterface<A, B, C> {}
}
