package tech.harmonysoft.oss.jenome.match.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

/**
 * @author Denis Zhdanov
 * @since 11/21/2009
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TypeVariableComplianceMatcherTest {

    private final TypeVariable<? extends GenericDeclaration> unboundTypeVariable = TestClass.class.getTypeParameters()[0];
    private final TypeVariable<? extends GenericDeclaration> boundTypeVariable = TestClass.class.getTypeParameters()[1];
    private TypeVariableComplianceMatcher matcher;

    @Before
    public void setUp() throws Exception {
        matcher = new TypeVariableComplianceMatcher();
    }

    @Test
    public void toParameterizedType() throws NoSuchFieldException {
        class TestClass {
            public Collection<Integer> field1;
            public Collection<Long> field2;
            public Collection<Number> field3;
            public Collection<Object> field4;
            public Comparable<String> field5;
            public Comparable<Comparable<Long>> field6;
            public Comparable<Comparable<Number>> field7;
            public Comparable<Comparable<String>> field8;
        }

        Type integerCollection = TestClass.class.getField("field1").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, integerCollection));
        assertFalse(matcher.match(boundTypeVariable, integerCollection));

        Type longCollection = TestClass.class.getField("field2").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, longCollection));
        assertFalse(matcher.match(boundTypeVariable, longCollection));

        Type numberCollection = TestClass.class.getField("field3").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, numberCollection));
        assertFalse(matcher.match(boundTypeVariable, numberCollection));

        Type objectCollection = TestClass.class.getField("field4").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, objectCollection));
        assertFalse(matcher.match(boundTypeVariable, objectCollection));

        Type stringComparable = TestClass.class.getField("field5").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, stringComparable));
        assertFalse(matcher.match(boundTypeVariable, stringComparable));

        Type comparableLong = TestClass.class.getField("field6").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, comparableLong));
        assertTrue(matcher.match(boundTypeVariable, comparableLong));

        Type comparableNumber = TestClass.class.getField("field7").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, comparableNumber));
        assertTrue(matcher.match(boundTypeVariable, comparableNumber));

        Type comparableString = TestClass.class.getField("field8").getGenericType();
        assertTrue(matcher.match(unboundTypeVariable, comparableString));
        assertFalse(matcher.match(boundTypeVariable, comparableString));
    }

    @Test
    public void toWildcardType() throws NoSuchFieldException {
        class TestClass {
            public Collection<Comparable<?>> field1;
            public Collection<Comparable<Comparable<?>>> field2;
            public Collection<? extends Comparable<Comparable<Integer>>> field3;
            public Collection<? extends Comparable<Comparable<? extends Long>>> field4;
            public Collection<? super Comparable<Comparable<Integer>>> field5;
            public Collection<? extends Comparable<Comparable<? super Long>>> field6;
            public Collection<? extends Comparable<Comparable<? super Number>>> field7;
        }

        Type comparableWildcard
                = ((ParameterizedType)TestClass.class.getField("field1").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableWildcard));
        assertFalse(matcher.match(boundTypeVariable, comparableWildcard));

        Type comparableComparableWildcard
                = ((ParameterizedType)TestClass.class.getField("field2").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableComparableWildcard));
        assertFalse(matcher.match(boundTypeVariable, comparableComparableWildcard));

        Type comparableComparableInteger
                = ((ParameterizedType)TestClass.class.getField("field3").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableComparableInteger));
        assertTrue(matcher.match(boundTypeVariable, comparableComparableInteger));

        Type comparableComparableUpperLong
                = ((ParameterizedType)TestClass.class.getField("field4").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableComparableUpperLong));
        assertTrue(matcher.match(boundTypeVariable, comparableComparableInteger));

        Type superComparableComparableInteger
                = ((ParameterizedType)TestClass.class.getField("field5").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, superComparableComparableInteger));
        assertFalse(matcher.match(boundTypeVariable, superComparableComparableInteger));

        Type comparableComparableSuperLong
                = ((ParameterizedType)TestClass.class.getField("field6").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableComparableSuperLong));
        assertFalse(matcher.match(boundTypeVariable, comparableComparableSuperLong));

        Type comparableComparableSuperNumber
                = ((ParameterizedType)TestClass.class.getField("field7").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, comparableComparableSuperNumber));
        assertFalse(matcher.match(boundTypeVariable, comparableComparableSuperNumber));
    }

    @Test
    public void toGenericArray() {
        class TestClass<T, U extends Comparable<Comparable<Long>>> extends GenericClass<T[], U[], Integer[]> {}

        Type unboundTypeVariableArray
                = ((ParameterizedType)TestClass.class.getGenericSuperclass()).getActualTypeArguments()[0];
        assertTrue(matcher.match(unboundTypeVariable, unboundTypeVariableArray));
        assertFalse(matcher.match(boundTypeVariable, unboundTypeVariableArray));

        Type boundTypeVariableArray
                = ((ParameterizedType)TestClass.class.getGenericSuperclass()).getActualTypeArguments()[1];
        assertTrue(matcher.match(unboundTypeVariable, unboundTypeVariableArray));
        assertFalse(matcher.match(boundTypeVariable, unboundTypeVariableArray));

        Type integerArray
                = ((ParameterizedType)TestClass.class.getGenericSuperclass()).getActualTypeArguments()[2];
        assertTrue(matcher.match(unboundTypeVariable, unboundTypeVariableArray));
        assertFalse(matcher.match(boundTypeVariable, unboundTypeVariableArray));
    }

    @Test
    public void toTypeVariable() {
        class TestClass<
                A,
                B extends Comparable<Comparable<? extends Number>>,
                C extends Comparable<Comparable<Long>>,
                D extends Comparable<Comparable<String>>
                > {}

        Type unbound = TestClass.class.getTypeParameters()[0];
        assertTrue(matcher.match(unboundTypeVariable, unbound));
        assertFalse(matcher.match(boundTypeVariable, unbound));

        Type boundToNumber = TestClass.class.getTypeParameters()[1];
        assertTrue(matcher.match(unboundTypeVariable, boundToNumber));
        assertTrue(matcher.match(boundTypeVariable, boundToNumber));

        Type boundToLong = TestClass.class.getTypeParameters()[2];
        assertTrue(matcher.match(unboundTypeVariable, boundToLong));
        assertTrue(matcher.match(boundTypeVariable, boundToLong));

        Type boundToString = TestClass.class.getTypeParameters()[3];
        assertTrue(matcher.match(unboundTypeVariable, boundToString));
        assertFalse(matcher.match(boundTypeVariable, boundToString));
    }

    @Test
    public void toClass() {
        class BoundToNumber implements Comparable<Comparable<? extends Number>> {
            @Override
            public int compareTo(Comparable<? extends Number> o) {
                return 0;
            }
        }
        class BoundToInteger implements Comparable<Comparable<Integer>> {
            @Override
            public int compareTo(Comparable<Integer> o) {
                return 0;
            }
        }
        class BoundToString implements Comparable<Comparable<String>> {
            @Override
            public int compareTo(Comparable<String> o) {
                return 0;
            }
        }

        assertTrue(matcher.match(unboundTypeVariable, BoundToNumber.class));
        assertTrue(matcher.match(boundTypeVariable, BoundToNumber.class));

        assertTrue(matcher.match(unboundTypeVariable, BoundToInteger.class));
        assertTrue(matcher.match(boundTypeVariable, BoundToInteger.class));

        assertTrue(matcher.match(unboundTypeVariable, BoundToString.class));
        assertFalse(matcher.match(boundTypeVariable, BoundToString.class));
    }

    class GenericClass<A, B, C> {}
    class TestClass<T, U extends Comparable<Comparable<? extends Number>>> {}
}