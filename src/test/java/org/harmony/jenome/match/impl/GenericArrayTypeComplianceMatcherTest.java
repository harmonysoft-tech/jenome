package org.harmony.jenome.match.impl;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.harmony.jenome.resolve.TypeArgumentResolver;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
@SuppressWarnings({"UnusedDeclaration"})
// TODO den remove
@Ignore
public class GenericArrayTypeComplianceMatcherTest {

    private GenericArrayTypeComplianceMatcher matcher;
    private GenericArrayType numberArrayType = (GenericArrayType)
            ((ParameterizedType) NumberArrayClass.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];
    private GenericArrayType objectArrayType = (GenericArrayType)
            ((ParameterizedType) ObjectArrayClass.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];

    @Before
    public void setUp() throws Exception {
        matcher = new GenericArrayTypeComplianceMatcher();
    }

// TODO den fix
//    @Test
    public void parameterizedType() {
        assertFalse(matcher.match(numberArrayType, NumberArrayClass.class.getGenericInterfaces()[0]));
    }

// TODO den fix
//    @Test
    public void wildcardType() throws NoSuchFieldException {
        class TestClass {
            public Collection<? extends Number> field1;
            public Collection<? extends Number[]> field2;
            public Collection<? extends Long[]> field3;
            public Collection<? super Long[]> field4;
            public Collection<? super Number[]> field5;
            public Collection<?> field6;
        }

        Type lowerToNumber
                = ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, lowerToNumber));
        assertFalse(matcher.match(objectArrayType, lowerToNumber));

        Type lowerToNumberArray
                = ((ParameterizedType)TestClass.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, lowerToNumberArray));
        assertFalse(matcher.match(objectArrayType, lowerToNumberArray));

        Type lowerToLongArray
                = ((ParameterizedType)TestClass.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, lowerToLongArray));
        assertFalse(matcher.match(objectArrayType, lowerToLongArray));

        Type upperToLongArray
                = ((ParameterizedType)TestClass.class.getField("field4").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, upperToLongArray));
        assertFalse(matcher.match(objectArrayType, upperToLongArray));

        Type upperToNumberArray
                = ((ParameterizedType)TestClass.class.getField("field5").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, upperToNumberArray));
        assertFalse(matcher.match(objectArrayType, upperToNumberArray));

        Type wildcard
                = ((ParameterizedType)TestClass.class.getField("field6").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(numberArrayType, wildcard));
        assertFalse(matcher.match(objectArrayType, wildcard));
    }

// TODO den fix
//    @Test
    public void toGenericArray() throws NoSuchFieldException {
        class TestClass<T> {
            public Collection<Integer[]> baseField1;
            public Collection<Long[]> baseField2;
            public Collection<List<? super Long>[]> baseField3;
            public Collection<List<? extends Number[]>[]> baseField4;

            public Collection<Integer> candidateField1;
            public Collection<Object[]> candidateField2;
            public Collection<Number[]> candidateField3;
            public Collection<List<? super Number>[]> candidateField4;
            public Collection<List<? super Integer>[]> candidateField5;
            public Collection<List<Integer[]>[]> candidateField6;
        }

        doTest(TestClass.class, "baseField1", "candidateField1", false);
        doTest(TestClass.class, "baseField1", "candidateField2", false);
        doTest(TestClass.class, "baseField2", "candidateField2", false);
        doTest(TestClass.class, "baseField2", "candidateField3", false);
        doTest(TestClass.class, "baseField3", "candidateField4", true);
        doTest(TestClass.class, "baseField3", "candidateField5", false);
        doTest(TestClass.class, "baseField4", "candidateField3", false);
        doTest(TestClass.class, "baseField4", "candidateField5", false);
        doTest(TestClass.class, "baseField4", "candidateField6", true);
    }

    private void doTest(Class<?> clazz, String baseFieldName, String candidateFieldName, boolean expectedMatch)
            throws NoSuchFieldException
    {
        GenericArrayType genericArrayType = (GenericArrayType) getGenericFieldType(clazz, baseFieldName);
        assertEquals(expectedMatch, matcher.match(genericArrayType, getGenericFieldType(clazz, candidateFieldName)));
    }

    private Type getGenericFieldType(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        ParameterizedType parameterizedType = (ParameterizedType) clazz.getField(fieldName).getGenericType();
        return parameterizedType.getActualTypeArguments()[0];
    }

// TODO den fix
//    @Test
    public void typeVariable() {
        class TestClass<T> {}
        assertFalse(matcher.match(numberArrayType, TestClass.class.getTypeParameters()[0]));
        assertFalse(matcher.match(objectArrayType, TestClass.class.getTypeParameters()[0]));
    }
// TODO den fix
//    @Test
    public void toClass() {
        assertFalse(matcher.match(numberArrayType, Number[].class));
        assertFalse(matcher.match(objectArrayType, Number[].class));
    }

// TODO den fix
//    @Test
    public void toType() {
        assertFalse(matcher.match(numberArrayType, TypeArgumentResolver.RAW_TYPE));
        assertFalse(matcher.match(objectArrayType, TypeArgumentResolver.RAW_TYPE));
    }

    interface TestInterface<A> {}
    class NumberArrayClass implements TestInterface<Number[]> {}
    class ObjectArrayClass implements TestInterface<Object[]> {}
}