package tech.harmonysoft.oss.jenome.match.impl;

import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"UnusedDeclaration"})
public class TopLevelTypeComplianceMatcherTest {

    private final Type dummyType = new Type() {};
    private TopLevelTypeComplianceMatcher matcher = new TopLevelTypeComplianceMatcher();

    @Test
    public void toParameterizedType() {
        assertTrue(matcher.match(TypeArgumentResolver.RAW_TYPE, TestInterfaceImpl.class.getGenericInterfaces()[0]));
        assertFalse(matcher.match(dummyType, TestInterfaceImpl.class.getGenericInterfaces()[0]));
    }

    @Test
    public void toWildcard() throws NoSuchFieldException {
        class TestClass {
            public Collection<?> field;
        }
        Type wildcard = ((ParameterizedType)TestClass.class.getField("field").getGenericType()).getActualTypeArguments()[0];
        assertTrue(matcher.match(TypeArgumentResolver.RAW_TYPE, wildcard));
        assertFalse(matcher.match(dummyType, wildcard));
    }

    @Test
    public void toTypeVariable() {
        class TestClass<T> {}
        assertTrue(matcher.match(TypeArgumentResolver.RAW_TYPE, TestClass.class.getTypeParameters()[0]));
        assertFalse(matcher.match(dummyType, TestClass.class.getTypeParameters()[0]));
    }

    @Test
    public void toClass() {
        assertTrue(matcher.match(TypeArgumentResolver.RAW_TYPE, Class.class));
        assertFalse(matcher.match(dummyType, Class.class));
    }

    @Test
    public void toType() {
        assertTrue(matcher.match(TypeArgumentResolver.RAW_TYPE, dummyType));
        assertFalse(matcher.match(dummyType, dummyType));
    }

    interface TestInterface<A> {}
    class TestInterfaceImpl<S> implements TestInterface<S> {}
}