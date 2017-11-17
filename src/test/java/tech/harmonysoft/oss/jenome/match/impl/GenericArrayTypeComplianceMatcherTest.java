package tech.harmonysoft.oss.jenome.match.impl;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class GenericArrayTypeComplianceMatcherTest {

    private GenericArrayTypeComplianceMatcher matcher = new GenericArrayTypeComplianceMatcher();

    private GenericArrayType collectionNumberArrayType = getType(CollectionNumberArrayClass.class);
    private GenericArrayType objectArrayType           = getType(CollectionObjectArrayClass.class);

    @NotNull
    private static GenericArrayType getType(@NotNull Class<?> clazz) {
        return (GenericArrayType) ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    @Test
    public void parameterizedType() {
        assertTrue(matcher.match(collectionNumberArrayType, getType(CollectionNumberArrayClass.class)));
        assertTrue(matcher.match(collectionNumberArrayType, getType(ListNumberArrayClass.class)));
        assertFalse(matcher.match(collectionNumberArrayType, getType(ListLongArrayClass.class)));
    }

    @Test
    public void wildcardType() throws NoSuchFieldException {
        class TestClass {
            public Collection<? extends Number>   field1;
            public Collection<? extends Number[]> field2;
            public Collection<? extends Long[]>   field3;
            public Collection<? super Long[]>     field4;
            public Collection<? super Number[]>   field5;
            public Collection<?>                  field6;
        }

        Type lowerToNumber
                = ((ParameterizedType) TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, lowerToNumber));
        assertFalse(matcher.match(objectArrayType, lowerToNumber));

        Type lowerToNumberArray
                = ((ParameterizedType) TestClass.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, lowerToNumberArray));
        assertFalse(matcher.match(objectArrayType, lowerToNumberArray));

        Type lowerToLongArray
                = ((ParameterizedType) TestClass.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, lowerToLongArray));
        assertFalse(matcher.match(objectArrayType, lowerToLongArray));

        Type upperToLongArray
                = ((ParameterizedType) TestClass.class.getField("field4").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, upperToLongArray));
        assertFalse(matcher.match(objectArrayType, upperToLongArray));

        Type upperToNumberArray
                = ((ParameterizedType) TestClass.class.getField("field5").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, upperToNumberArray));
        assertFalse(matcher.match(objectArrayType, upperToNumberArray));

        Type wildcard
                = ((ParameterizedType) TestClass.class.getField("field6").getGenericType()).getActualTypeArguments()[0];
        assertFalse(matcher.match(collectionNumberArrayType, wildcard));
        assertFalse(matcher.match(objectArrayType, wildcard));
    }

    @Test
    public void toGenericArray() throws NoSuchFieldException {
        class TestClass<T> {
            public Collection<List<? super Long>[]>       baseField1;
            public Collection<List<? extends Number[]>[]> baseField2;

            public Collection<Number[]>                candidateField1;
            public Collection<List<? super Number>[]>  candidateField2;
            public Collection<List<? super Integer>[]> candidateField3;
            public Collection<List<Integer[]>[]>       candidateField4;
        }

        doTest(TestClass.class, "baseField1", "candidateField2", true);
        doTest(TestClass.class, "baseField1", "candidateField3", false);
        doTest(TestClass.class, "baseField2", "candidateField1", false);
        doTest(TestClass.class, "baseField2", "candidateField3", false);
        doTest(TestClass.class, "baseField2", "candidateField4", true);
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

    @Test
    public void typeVariable() {
        class TestClass<T> {}
        assertFalse(matcher.match(collectionNumberArrayType, TestClass.class.getTypeParameters()[0]));
        assertFalse(matcher.match(objectArrayType, TestClass.class.getTypeParameters()[0]));
    }

    @Test
    public void toClass() {
        assertFalse(matcher.match(collectionNumberArrayType, Number[].class));
        assertFalse(matcher.match(objectArrayType, Number[].class));
    }

    @Test
    public void toType() {
        assertFalse(matcher.match(collectionNumberArrayType, TypeArgumentResolver.RAW_TYPE));
        assertFalse(matcher.match(objectArrayType, TypeArgumentResolver.RAW_TYPE));
    }

    interface TestInterface<A> {}

    class CollectionNumberArrayClass implements TestInterface<Collection<Number>[]> {}

    class ListNumberArrayClass implements TestInterface<List<Number>[]> {}

    class ListLongArrayClass implements TestInterface<List<Long>[]> {}

    class CollectionObjectArrayClass implements TestInterface<Collection<Object>[]> {}
}