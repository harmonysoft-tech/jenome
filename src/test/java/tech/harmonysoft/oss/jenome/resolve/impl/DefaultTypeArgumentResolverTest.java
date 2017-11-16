package tech.harmonysoft.oss.jenome.resolve.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"RawUseOfParameterizedType", "unchecked", "UnusedDeclaration", "serial"})
public class DefaultTypeArgumentResolverTest {

    private final ParameterizedType testInterfaceType
            = (ParameterizedType) TestInterfaceImpl.class.getGenericInterfaces()[0];
    private final ParameterizedType testInterfaceImplType
            = (ParameterizedType) ParameterizedChild.class.getGenericSuperclass();
    private DefaultTypeArgumentResolver resolver;

    @BeforeEach
    public void setUp() throws Exception {
        resolver = new DefaultTypeArgumentResolver();
    }

    @Test
    public void toWildCardType() throws NoSuchFieldException {
        class Test {
            public Collection<? extends Comparable<Number>> field;
        }

        Type type = ((ParameterizedType)Test.class.getField("field").getGenericType()).getActualTypeArguments()[0];
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(type, Integer.class, 0));
    }

    @Test
    public void toGenericArrayType() throws NoSuchFieldException {
        class Test<T> implements TestInterface<T[], Integer, String>{}

        Type type = ((ParameterizedType)Test.class.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(type, Integer.class, 0));
    }

    @Test
    public void toTypeVariable() throws NoSuchFieldException {
        class Test<T>{}

        Type type = Test.class.getTypeParameters()[0];
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(type, Integer.class, 0));
    }

    @Test
    public void toVariable() throws NoSuchFieldException {
        Type type = new Type() {};
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(type, Integer.class, 0));
    }

    @Test
    public void toIncompatibleTypes() {
        assertThrows(IllegalArgumentException.class,
                     () -> resolver.resolve(TestInterface.class, Integer.class, 0));
    }

    @Test
    public void negativeIndex() {
        assertThrows(IllegalArgumentException.class, () ->resolver.resolve(Comparable.class, Integer.class, -1));
    }

    @Test
    public void indexOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(Comparable.class, Integer.class, 1));
    }

    @Test
    public void toParameterizedDirectInterface() {
        class Sub1<X, Y, Z> implements TestInterface<Y, Z, X> {}
        class Sub2<D, E> extends Sub1<String, E, D> {}
        class Sub3 extends Sub2<Integer, Long> {}

        assertSame(Long.class, resolver.resolve(testInterfaceType, Sub3.class, 0));
        assertSame(Long.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 0));
        assertSame(Integer.class, resolver.resolve(testInterfaceType, Sub3.class, 1));
        assertSame(Integer.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 1));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub3.class, 2));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 2));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub2.class, 2));

        Type resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 0);
        assertTrue(resolvedType instanceof TypeVariable);

        TypeVariable<?> typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("E", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());

        resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 1);
        assertTrue(resolvedType instanceof TypeVariable);

        typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("D", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());
    }

    @Test
    public void toParameterizedIndirectInterface() {
        class Sub1<A, B, C> implements SubInterface2<A, C, B> {}
        class Sub2<A, C> extends Sub1<String, C, A> implements Comparable<A>{
            @Override
            public int compareTo(@NotNull A o) {
                return 0;
            }
        }
        class Sub3 extends Sub2<Integer, Long> implements Serializable {}

        assertSame(Long.class, resolver.resolve(testInterfaceType, Sub3.class, 0));
        assertSame(Long.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 0));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub3.class, 1));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 1));
        assertSame(Integer.class, resolver.resolve(testInterfaceType, Sub3.class, 2));
        assertSame(Integer.class, resolver.resolve(testInterfaceType, Sub3.class.getGenericSuperclass(), 2));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub2.class, 1));

        Type resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 0);
        assertTrue(resolvedType instanceof TypeVariable);

        TypeVariable<?> typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("C", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());

        resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 2);
        assertTrue(resolvedType instanceof TypeVariable);

        typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("A", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());
    }

    @Test
    public void toValidParameterizedClass() {
        class Sub1<X, Y, Z> extends TestInterfaceImpl<Y, Z, X> {}
        class Sub2<D, E> extends Sub1<String, E, D> {}
        class Sub3 extends Sub2<Integer, Long> {}

        assertSame(Long.class, resolver.resolve(testInterfaceImplType, Sub3.class, 0));
        assertSame(Long.class, resolver.resolve(testInterfaceImplType, Sub3.class.getGenericSuperclass(), 0));
        assertSame(Integer.class, resolver.resolve(testInterfaceImplType, Sub3.class, 1));
        assertSame(Integer.class, resolver.resolve(testInterfaceImplType, Sub3.class.getGenericSuperclass(), 1));
        assertSame(String.class, resolver.resolve(testInterfaceImplType, Sub3.class, 2));
        assertSame(String.class, resolver.resolve(testInterfaceImplType, Sub3.class.getGenericSuperclass(), 2));
        assertSame(String.class, resolver.resolve(testInterfaceType, Sub2.class, 2));

        Type resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 0);
        assertTrue(resolvedType instanceof TypeVariable);

        TypeVariable<?> typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("E", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());

        resolvedType = resolver.resolve(testInterfaceType, Sub2.class, 1);
        assertTrue(resolvedType instanceof TypeVariable);

        typeVariable = (TypeVariable<?>) resolvedType;
        assertEquals("D", typeVariable.getName());
        assertSame(Sub2.class, typeVariable.getGenericDeclaration());
    }

    @Test
    public void toRawInterface() {
        class TestClass implements SubInterface2 {}

        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceType, TestClass.class, 0));
        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceType, TestClass.class, 1));
        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceType, TestClass.class, 2));
    }

    @Test
    public void toRawClass() {

        class TestClass extends ParameterizedChild {}

        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceImplType, TestClass.class, 0));
        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceImplType, TestClass.class, 1));
        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(testInterfaceImplType, TestClass.class, 2));
        assertSame(TypeArgumentResolver.RAW_TYPE, resolver.resolve(Comparable.class, Comparable.class, 0));
    }

    @Test
    public void classToInterface() throws NoSuchFieldException {
        class TestClass {
            public List<Integer> field;
        }
        assertSame(Integer.class, resolver.resolve(Collection.class, TestClass.class.getField("field").getGenericType(), 0));
    }

    @Test
    public void parameterizedClassToParameterizedInterface() throws NoSuchFieldException {
        class TestClass {
            public Collection<Long> field1;
            public List<Integer> field2;
        }
        assertSame(
                Integer.class,
                resolver.resolve(
                        TestClass.class.getField("field1").getGenericType(),
                        TestClass.class.getField("field2").getGenericType(),
                        0
                )
        );
    }

    private interface TestInterface<A, B, C> {}
    private interface SubInterface1<A, B, C> extends TestInterface<B, C, A> {}
    private interface SubInterface2<A, B, C> extends SubInterface1<B, C, A> {}
    private class TestInterfaceImpl<A, B, C> implements TestInterface<A, B, C> {}
    private class ParameterizedChild<A, B, C> extends TestInterfaceImpl<A, B, C> {}
}