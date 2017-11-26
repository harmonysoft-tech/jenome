package tech.harmonysoft.oss.jenome.resolve.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JenomeResolveUtilTest {

    public interface MyInterface<T> {}
    public static class IntHandler implements MyInterface<Integer> {}
    public static class LongHandler implements MyInterface<Long> {}
    public static class ParameterizedHandler<T> implements MyInterface<T> {}
    public static class RawHandler implements MyInterface {}

    @Test
    public void resolve_success() {
        Map<Type, MyInterface<?>> expected = new HashMap<>();
        expected.put(Integer.class, new IntHandler());
        expected.put(Long.class, new LongHandler());
        Map<Type, MyInterface<?>> actual = expected.values().stream()
                                                   .collect(toMap(JenomeResolveUtil::getTypeArgument,
                                                                  Function.identity()));
        assertEquals(expected, actual);
    }

    @Test
    public void resolve_failure_rawType() {
        assertThrows(IllegalArgumentException.class, () -> {
            JenomeResolveUtil.getTypeArgument(new RawHandler());
        });
    }

    @Test
    public void resolve_failure_parameterizedType() {
        assertThrows(IllegalArgumentException.class, () -> {
            JenomeResolveUtil.getTypeArgument(new ParameterizedHandler());
        });
    }
}