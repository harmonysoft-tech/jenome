package tech.harmonysoft.oss.jenome.match.impl;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;

@SuppressWarnings({"UnusedDeclaration", "serial"})
public class WildcardTypeComplianceMatcherTest {

    private final WildcardType wildcard;
    private final WildcardType boundToNumber;
    private final WildcardType boundToTypeVariable;
    private final WildcardType boundToLong;
    private WildcardTypeComplianceMatcher matcher;

    public WildcardTypeComplianceMatcherTest() throws NoSuchFieldException {
        wildcard = (WildcardType) ((ParameterizedType)
                TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        boundToNumber = (WildcardType) ((ParameterizedType)
                TestClass.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        boundToTypeVariable = (WildcardType) ((ParameterizedType)
                TestClass.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        boundToLong = (WildcardType) ((ParameterizedType)
                TestClass.class.getField("field4").getGenericType()).getActualTypeArguments()[0];
    }

    @Before
    public void setUp() throws Exception {
        matcher = new WildcardTypeComplianceMatcher();
    }

    @Test
    public void toParameterizedType() throws NoSuchFieldException {
        class TestClass {
            public List<? extends Integer> field1;
            public Collection<Object> field2;
            public Collection<Comparable<String>> field3;
            public Collection<String> field4;
            public List<Object> field5;
        }

        Type boundToInteger = TestClass.class.getField("field1").getGenericType();
        assertFalse(matcher.match(wildcard, boundToInteger));
        assertTrue(matcher.match(boundToNumber, boundToInteger));
        assertFalse(matcher.match(boundToTypeVariable, boundToInteger));
        assertFalse(matcher.match(boundToLong, boundToInteger));

        Type boundToObject = TestClass.class.getField("field2").getGenericType();
        assertFalse(matcher.match(wildcard, boundToObject));
        assertFalse(matcher.match(boundToNumber, boundToObject));
        assertFalse(matcher.match(boundToTypeVariable, boundToObject));
        assertTrue(matcher.match(boundToLong, boundToObject));

        Type comparableString = TestClass.class.getField("field3").getGenericType();
        assertFalse(matcher.match(wildcard, comparableString));
        assertFalse(matcher.match(boundToNumber, comparableString));
        assertTrue(matcher.match(boundToTypeVariable, comparableString));
        assertFalse(matcher.match(boundToLong, comparableString));

        Type boundToString = TestClass.class.getField("field4").getGenericType();
        assertFalse(matcher.match(wildcard, boundToString));
        assertFalse(matcher.match(boundToNumber, boundToString));
        assertFalse(matcher.match(boundToTypeVariable, boundToString));
        assertFalse(matcher.match(boundToLong, boundToString));

        Type objectList = TestClass.class.getField("field5").getGenericType();
        assertFalse(matcher.match(wildcard, objectList));
        assertFalse(matcher.match(boundToNumber, objectList));
        assertFalse(matcher.match(boundToTypeVariable, objectList));
        assertFalse(matcher.match(boundToLong, objectList));
    }

    @Test
    public void toWildcard() throws NoSuchFieldException {
        class TestClass {
            public Collection<? extends List<? super Long>> field1;
            public Collection<? extends List<? extends Long>> field2;
            public Collection<? super Collection<? extends Number>> field3;
            public Collection<? super Collection<? super Number>> field4;
            public Collection<? super List<? super Number>> field5;
        }

        Type listSuperLong
                = ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(wildcard, listSuperLong));
        assertFalse(matcher.match(boundToNumber, listSuperLong));
        assertFalse(matcher.match(boundToTypeVariable, listSuperLong));
        assertFalse(matcher.match(boundToLong, listSuperLong));

        Type listExtendsLong
                = ((ParameterizedType)TestClass.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(wildcard, listExtendsLong));
        assertTrue(matcher.match(boundToNumber, listExtendsLong));
        assertFalse(matcher.match(boundToTypeVariable, listExtendsLong));
        assertFalse(matcher.match(boundToLong, listExtendsLong));

        Type collectionExtendsNumber
                = ((ParameterizedType)TestClass.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(wildcard, collectionExtendsNumber));
        assertFalse(matcher.match(boundToNumber, collectionExtendsNumber));
        assertFalse(matcher.match(boundToTypeVariable, collectionExtendsNumber));
        assertFalse(matcher.match(boundToLong, collectionExtendsNumber));

        Type collectionSuperNumber
                = ((ParameterizedType)TestClass.class.getField("field4").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(wildcard, collectionSuperNumber));
        assertFalse(matcher.match(boundToNumber, collectionSuperNumber));
        assertFalse(matcher.match(boundToTypeVariable, collectionSuperNumber));
        assertTrue(matcher.match(boundToLong, collectionSuperNumber));

        WildcardType superListSuperNumber = (WildcardType) ((ParameterizedType)
                TestClass.class.getField("field5").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(superListSuperNumber, collectionSuperNumber));
    }

    @Test
    public void toWildcardWithArrays() throws NoSuchFieldException {
        class TestClass<T> {
            public Collection<? extends Integer[]> baseField1;
            public Collection<? super Long[]> baseField2;
            public Collection<? extends Collection<? super Long[]>[]> baseField3;
            public Collection<? super List<? extends Number[]>[]> baseField4;

            public Collection<? extends Integer> candidateField1;
            public Collection<? extends Object[]> candidateField2;
            public Collection<? super Number[]> candidateField3;
            public Collection<? extends List<? super Number[]>[]> candidateField4;
            public Collection<? extends List<? super Integer>[]> candidateField5;
            public Collection<? super Collection<Integer[]>[]> candidateField6;
        }

        doTest(TestClass.class, "baseField1", "candidateField1", false);
        doTest(TestClass.class, "baseField1", "candidateField2", false);
        doTest(TestClass.class, "baseField2", "candidateField2", false);
        doTest(TestClass.class, "baseField2", "candidateField3", true);
        doTest(TestClass.class, "baseField3", "candidateField4", true);
        doTest(TestClass.class, "baseField3", "candidateField5", false);
        doTest(TestClass.class, "baseField4", "candidateField3", false);
        doTest(TestClass.class, "baseField4", "candidateField5", false);
        doTest(TestClass.class, "baseField4", "candidateField6", true);
    }

    private void doTest(Class<?> clazz, String baseFieldName, String candidateFieldName, boolean expectedMatch)
            throws NoSuchFieldException
    {
        WildcardType wildcardType = (WildcardType) getGenericFieldType(clazz, baseFieldName);
        assertEquals(expectedMatch, matcher.match(wildcardType, getGenericFieldType(clazz, candidateFieldName)));
    }

    private Type getGenericFieldType(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        ParameterizedType parameterizedType = (ParameterizedType) clazz.getField(fieldName).getGenericType();
        return parameterizedType.getActualTypeArguments()[0];
    }

    @Test
    public void toGenericArray() throws NoSuchFieldException {
        class TestClass<T> {
            public Collection<? extends Collection<Integer>[]> field1;
            public Collection<? super Collection<Long>[]> field2;
        }
        assertFalse(matcher.match(
                wildcard,
                ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0]
        ));
        assertFalse(matcher.match(
                boundToNumber,
                ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0]
        ));
        assertFalse(matcher.match(
                boundToLong,
                ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0]
        ));
    }

    @Test
    public void toTypeVariable() {
        class TestClass<
                A extends List<? extends Integer>,
                B extends List<? super Long>,
                C extends Collection<Comparable<?>>,
                D
                > {}
        TypeVariable<Class<TestClass>>[] typeVariables = TestClass.class.getTypeParameters();

        assertFalse(matcher.match(wildcard, typeVariables[0]));
        assertFalse(matcher.match(wildcard, typeVariables[1]));
        assertFalse(matcher.match(wildcard, typeVariables[2]));
        assertFalse(matcher.match(wildcard, typeVariables[3]));

        assertTrue(matcher.match(boundToNumber, typeVariables[0]));
        assertFalse(matcher.match(boundToNumber, typeVariables[1]));
        assertFalse(matcher.match(boundToNumber, typeVariables[3]));

        assertFalse(matcher.match(boundToTypeVariable, typeVariables[0]));
        assertTrue(matcher.match(boundToTypeVariable, typeVariables[2]));
        assertFalse(matcher.match(boundToTypeVariable, typeVariables[3]));
    }

    @Test
    public void toClass() {
        class TestClass1 extends ArrayList<Long> {}
        class TestClass2 extends HashSet<String> {}
        class TestClass3 extends TreeSet<Comparable<String>> {}
        class TestClass4 extends LinkedList<Comparable<Number>> {}

        assertFalse(matcher.match(wildcard, TestClass1.class));
        assertFalse(matcher.match(wildcard, TestClass2.class));

        assertTrue(matcher.match(boundToNumber, TestClass1.class));
        assertFalse(matcher.match(boundToNumber, TestClass2.class));

        assertTrue(matcher.match(boundToTypeVariable, TestClass1.class));
        assertTrue(matcher.match(boundToTypeVariable, TestClass2.class));
        assertTrue(matcher.match(boundToTypeVariable, TestClass3.class));
        assertTrue(matcher.match(boundToTypeVariable, TestClass4.class));

        assertFalse(matcher.match(boundToLong, TestClass1.class));
        assertFalse(matcher.match(boundToLong, TestClass2.class));
        assertFalse(matcher.match(boundToLong, TestClass3.class));
        assertFalse(matcher.match(boundToLong, TestClass4.class));
    }

    static class TestClass<T> {
        public Collection<?> field1;
        public Collection<? extends Collection<? extends Number>> field2;
        public Collection<? extends Collection<Comparable<T>>> field3;
        public Collection<? super Collection<? super Long>> field4;
    }
}