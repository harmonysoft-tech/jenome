package tech.harmonysoft.oss.jenome.resolve.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import static org.junit.Assert.*;

/**
 * @author Denis Zhdanov
 */
@SuppressWarnings({"UnusedDeclaration"})
public class GenericsHelperTest {

    private GenericsHelper helper;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        helper = new GenericsHelper();
    }

    @Test(expected = IllegalArgumentException.class)
    public void inconsistentClassAndInterface() {
        helper.resolveTypeParameterValue(Comparable.class, new Object(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooBigTypeParameterIndex() {
        helper.resolveTypeParameterValue(Comparable.class, "", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeTypeParameterIndex() {
        helper.resolveTypeParameterValue(Comparable.class, "", -1);
    }

    @Test
    public void directNonGenericImplementation() {
        class Test implements Comparable<String> {
            @Override
            public int compareTo(String o) {
                return 0;
            }
        }
        assertEquals(String.class, helper.resolveTypeParameterValue(Comparable.class, new Test(), 0));
    }

    @Test
    public void directGenericImplementation() {
        class Test<T> implements Comparable<T> {
            @Override
            public int compareTo(T o) {
                return 0;
            }
        }
        Type type = helper.resolveTypeParameterValue(Comparable.class, new Test(), 0);
        assertTrue(type instanceof TypeVariable);

        TypeVariable typeVariable = (TypeVariable) type;
        assertEquals("T", typeVariable.getName());
    }

    @Test
    public void indirectNonGenericImplementation() {
        class Parent<T> implements Comparable<T> {
            @Override
            public int compareTo(T o) {
                return 0;
            }
        }
        class Child extends Parent<Integer> {}
        assertEquals(Integer.class, helper.resolveTypeParameterValue(Comparable.class, new Child(), 0));
    }

    @Test
    public void indirectGenericImplementation() {
        class Parent<T> implements Comparable<T> {
            @Override
            public int compareTo(T o) {
                return 0;
            }
        }
        class Child<S> extends Parent<S> {}

        Type type = helper.resolveTypeParameterValue(Comparable.class, new Child(), 0);
        assertTrue(type instanceof TypeVariable);

        TypeVariable typeVariable = (TypeVariable) type;
        assertEquals("S", typeVariable.getName());
    }

    @Test
    public void mixedNonGenericTypeVariablesOrder() {
        class Parent<F, S, T> implements Comparable<S> {
            @Override
            public int compareTo(S o) {
                return 0;
            }
        }
        class Child1<FC1, FS1> extends Parent<FS1, FC1, Integer> {}
        class Child2 extends Child1<Long, String> {}

        assertEquals(Long.class, helper.resolveTypeParameterValue(Comparable.class, new Child2(), 0));
    }

    @Test
    public void mixedGenericTypeVariablesOrder() {
        class Parent<F, S, T> implements Comparable<S> {
            @Override
            public int compareTo(S o) {
                return 0;
            }
        }
        class Child1<FC1, FS1> extends Parent<FS1, FC1, Integer> {}
        class Child2<FC2, FS2> extends Child1<FS2, FC2> {}
        class Child3<FC2, FS2> extends Child1<File, FC2> {}

        Type type = helper.resolveTypeParameterValue(Comparable.class, new Child2(), 0);
        assertTrue(type instanceof TypeVariable);

        TypeVariable typeVariable = (TypeVariable) type;
        assertEquals("FS2", typeVariable.getName());

        assertEquals(File.class, helper.resolveTypeParameterValue(Comparable.class, new Child3(), 0));
    }

    @Test
    public void rawGenericImplementationClassType() {
        class Parent<T> implements Comparable<T> {
            @Override
            public int compareTo(T o) {
                return 0;
            }
        }
        class Child extends Parent {}
        assertEquals(Object.class, helper.resolveTypeParameterValue(Comparable.class, new Child(), 0));
    }

    @Test
    public void rawInterfaceAndGenericImplementation() {
        class Test<T> implements Comparable {
            @Override
            public int compareTo(Object o) {
                return 0;
            }
        }
        assertEquals(Object.class, helper.resolveTypeParameterValue(Comparable.class, new Test(), 0));
    }

    @Test
    public void rawInterfaceAndNonGenericImplementation() {
        class Test implements Comparable {
            @Override
            public int compareTo(Object o) {
                return 0;
            }
        }
        assertEquals(Object.class, helper.resolveTypeParameterValue(Comparable.class, new Test(), 0));
    }

    @Test
    public void boundWildcardGenericImplementation() {
        class Test<T extends Number> implements Comparable<T> {
            @Override
            public int compareTo(T o) {
                return 0;
            }
        }
        Type type = helper.resolveTypeParameterValue(Comparable.class, new Test(), 0);
        assertTrue(type instanceof TypeVariable);

        TypeVariable typeVariable = (TypeVariable) type;
        assertEquals("T", typeVariable.getName());
        assertSame(Number.class, typeVariable.getBounds()[0]);
    }

    @Test
    public void genericArrayType() {
        class Test<T> implements Comparable<T[]> {
            @Override
            public int compareTo(T[] o) {
                return 0;
            }
        }
        Type type = helper.resolveTypeParameterValue(Comparable.class, new Test(), 0);
        assertTrue(type instanceof GenericArrayType);

        GenericArrayType genericArrayType = (GenericArrayType) type;
        assertTrue(genericArrayType.getGenericComponentType() instanceof TypeVariable);

        TypeVariable typeVariable = (TypeVariable) genericArrayType.getGenericComponentType();
        assertEquals("T", typeVariable.getName());
    }

    @Test
    public void correctIndexProcessing() {
        class Parent<X, Y, Z> implements TestInterface<Z, X, Y> {}
        class Child extends Parent<Integer, Long, Boolean> {}
        assertEquals(Boolean.class, helper.resolveTypeParameterValue(TestInterface.class, new Child(), 0));
        assertEquals(Integer.class, helper.resolveTypeParameterValue(TestInterface.class, new Child(), 1));
        assertEquals(Long.class, helper.resolveTypeParameterValue(TestInterface.class, new Child(), 2));
    }

    private interface TestInterface<A, B, C> {}
}