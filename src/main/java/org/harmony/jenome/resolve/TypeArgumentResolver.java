package org.harmony.jenome.resolve;

import java.lang.reflect.Type;

/**
 * Defines general contract for resolving type arguments.
 * <p/>
 * Implementations of this interface are assumed to be thread-safe.
 *
 * @author Denis Zhdanov
 */
public interface TypeArgumentResolver {

    /** Stands for a marker of a raw type */
    public static final Type RAW_TYPE = new Type() {
        @Override
        public String toString() {
            return "TypeArgumentResolver.RAW_TYPE (raw type indicator)";
        }
    };

    /**
     * Assumes that given <code>'base'</code> type is a parameterized type and given
     * <code>'target' IS-A 'base'</code> and tries to resolve type argument of the <code>'base'</code>
     * type for the given index.
     * <p/>
     * E.g. consider the following example:
     * <pre>
     *     public class Base<T> {}
     *
     *     public class Sub1<F, S> extends Base<S> {}
     *
     *     public class TestClass extends Sub<Integer, String> {}
     * </pre>
     * <ul>
     *     <li>resolve(Base.class, TestClass.class, 0) is String.class;</li>
     *     <li>resolve(Sub1.class, TestClass.class, 0) is String.class;</li>
     *     <li>resolve(Sub1.class, TestClass.class, 1) is Integer.class;</li>
     * </ul>
     * <p/>
     * There is a possible case that 'raw' implementation class is used instead of generic one
     * (e.g. consider the following hierarchy:
     * <pre>
     *     class Parent<T> implements Comparable<T> { // ... implementation}
     *
     *     class Child extends Parent {}
     * </pre>
     * Here <code>'Child'</code> class uses <code>'raw'</code> version of <code>'Parent'</code> class).
     * {@link #RAW_TYPE} is returned then.
     *
     * @param base      target base type that has type arguments
     * @param target    type that <code>IS-A 'base'</code> type
     * @param index     target index of the <code>'base'</code> type type parameter to resolve
     * @return          <code>'base'</code> type argument resolved against the given <code>'target'</code> type
     * @throws IllegalArgumentException     if any of the given arguments is <code>null</code> or if given
     *                                      <code>'base'</code> type doesn't have type argument for the given
     *                                      index (or doesn't have type arguments at all) or if given
     *                                      <code>'target'</code> type is not <code>IS-A 'base'</code> type
     */
    Type resolve(Type base, Type target, int index) throws IllegalArgumentException;
}