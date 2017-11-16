package org.harmony.jenome.resolve;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * <p>Defines general contract for resolving type arguments.</p>
 * <p>Implementations of this interface are assumed to be thread-safe.</p>
 */
public interface TypeArgumentResolver {

    /** Stands for a marker of a raw type */
    Type RAW_TYPE = new Type() {
        @Override
        public String toString() {
            return "TypeArgumentResolver.RAW_TYPE (raw type indicator)";
        }
    };

    /**
     * <p>
     *      Assumes that given {@code 'base'} type is a parameterized type and given
     *      {@code 'target' IS-A 'base'} and tries to resolve type argument of the {@code 'base'}
     *      type for the given index.
     * </p>
     * <p>E.g. consider the following example:</p>
     * <pre>
     *     public class Base<T> {}
     *
     *     public class Sub<F, S> extends Base<S> {}
     *
     *     public class TestClass extends Sub<Integer, String> {}
     * </pre>
     * <ul>
     *     <li>resolve(Base.class, TestClass.class, 0) is String.class;</li>
     *     <li>resolve(Sub.class, TestClass.class, 0) is Integer.class;</li>
     *     <li>resolve(Sub.class, TestClass.class, 1) is String.class;</li>
     * </ul>
     * <p>
     *      There is a possible case that 'raw' implementation class is used instead of generic one
     *      (e.g. consider the following hierarchy:
     * </p>
     * <pre>
     *     class Parent<T> implements Comparable<T> { // ... implementation}
     *
     *     class Child extends Parent {}
     * </pre>
     * <p>
     *      Here {@code 'Child'} class uses {@code 'raw'} version of the {@code 'Parent'} class).
     *      {@link #RAW_TYPE} is returned then.
     * </p>
     *
     * @param base      target base type that has type arguments
     * @param target    type that {@code IS-A 'base'} type
     * @param index     target index of the {@code 'base'} type type parameter to resolve
     * @return          {@code 'base'} type argument resolved against the given {@code 'target'} type
     * @throws IllegalArgumentException     if given {@code 'base'} type doesn't have type argument for the given
     *                                      index (or doesn't have type arguments at all) or if given
     *                                      {@code 'target'} type is not {@code IS-A 'base'} type
     */
    @NotNull
    Type resolve(@NotNull Type base, @NotNull Type target, int index) throws IllegalArgumentException;
}