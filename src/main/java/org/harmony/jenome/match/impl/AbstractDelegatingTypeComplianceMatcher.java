package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * <p>Expands {@link AbstractTypeComplianceMatcher} in order to add {@code 'matcher delegate'} concept.</p>
 * <p>
 *      {@code 'Matcher delegate'} here means {@link TypeComplianceMatcher} implementation that allows to handle
 *      compliance rules that are not handled by current {@code AbstractDelegatingTypeComplianceMatcher} subclass.
 * </p>
 * <p>
 *      E.g. suppose that current subclass contains logic that allows to check compliance rules when basic type
 *      is {@link ParameterizedType}. It's called when we need to check if {@code Comparable<Integer>} can be used
 *      in place of {@code Comparable<? extends Number>}. So, we check that raw types of given parameterized types
 *      are consistent and need to check {@code ? extends Number} wildcard type to {@code Integer} class. That logic
 *      is not contained at current subclass (remember, it contains comparison logic for the cases when base type is
 *      {@link ParameterizedType} not {@link WildcardType}), so, target type argument values are derived
 *      and the processing is delegated to another {@link TypeComplianceMatcher} implementation.
 * </p>
 * Thread-safe.
 */
public abstract class AbstractDelegatingTypeComplianceMatcher<T extends Type> extends AbstractTypeComplianceMatcher<T> {

    private final TypeComplianceMatcher<Type> delegate;

    /**
     * Creates new {@code AbstractDelegatingTypeComplianceMatcher} object with default
     * delegate ({@link CompositeTypeComplianceMatcher#INSTANCE}).
     */
    protected AbstractDelegatingTypeComplianceMatcher() {
        this(new CompositeTypeComplianceMatcher());
    }

    /**
     * Creates new {@code AbstractDelegatingTypeComplianceMatcher} object with given delegate.
     *
     * @param delegate      delegate to expose via {@link #getDelegate()} method
     */
    protected AbstractDelegatingTypeComplianceMatcher(@NotNull TypeComplianceMatcher<Type> delegate) {
        this.delegate = delegate;
    }

    /**
     * Allows to retrieve type compliance matcher delegate to use.
     *
     * @return      type compliance matcher delegate to use
     */
    @NotNull
    public TypeComplianceMatcher<Type> getDelegate() {
        return delegate;
    }
}