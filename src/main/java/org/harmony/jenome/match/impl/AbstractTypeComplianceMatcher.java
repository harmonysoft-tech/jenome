package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeArgumentResolver;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.DefaultTypeArgumentResolver;
import org.harmony.jenome.resolve.util.TypeDispatcher;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link TypeComplianceMatcher} implementation that is based on GoF <code>'Template Method'</code> pattern.
 * <p/>
 * I.e. this class defines general algorithm, offers useful facilities for subclasses and requires them to implement
 * particular functionality.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @param <T>   target <code>'base'</code> type
 * @see #match(Type, Type, boolean)
 */
public abstract class AbstractTypeComplianceMatcher<T extends Type> implements TypeComplianceMatcher<T> {

    /**
     * Holds stack of flags that indicate if <code>'strict'</code> check is performed
     * (check {@link #match(Type, Type, boolean)}) contract for more details.
     * <p/>
     * We use static variable here in order to be able to keep track of <code>'strict'</code> value across
     * multiple instances of underlying classes.
     */
    private static final ThreadLocal<Stack<Boolean>> STRICT = new ThreadLocal<Stack<Boolean>>();
    static {
        Stack<Boolean> stack = new Stack<Boolean>();
        stack.push(false);
        STRICT.set(stack);
    }

    /**
     * Stores <code>'base'</code> type used in comparison. That type is available to actual implementations
     * via {@link #getBaseType()} method.
     * <p/>
     * We use stack of values here in order to be able to handle the situation when the same matcher implementation
     * is used more than one during the same type comparison. Example of such a situation is comparison of
     * {@code Comparable<Collection<Comparable<? extends Number>>>} vs
     * {@code Comparable<Collection<Comparable<Long>>>}. Matcher that works with {@link ParameterizedType} is used
     * for different types here ({@link Comparable} and {@link Collection}), so, we need to keep track of base
     * type between those comparisons.
     */
    private final ThreadLocal<Stack<T>>                 baseType             = new ThreadLocal<Stack<T>>() {
        @Override
        protected Stack<T> initialValue() {
            return new Stack<T>();
        }
    };
    private final ThreadLocal<Boolean>                  matched              = new ThreadLocal<Boolean>();
    private final AtomicReference<TypeArgumentResolver> typeArgumentResolver = new AtomicReference<TypeArgumentResolver>();
    private final TypeDispatcher                        typeDispatcher       = new TypeDispatcher();

    protected AbstractTypeComplianceMatcher() {
        matched.set(false);
        setTypeArgumentResolver(DefaultTypeArgumentResolver.INSTANCE);
    }

    /** {@inheritDoc} */
    @Override
    public boolean match(T base, Type candidate) throws IllegalArgumentException {
        return match(base, candidate, false);
    }

    /**
     * Template method that defines basic match algorithm:
     * <ol>
     *     <li>
     *          given <code>'base'</code> type is remembered at thread-local variable and is available
     *          to subclasses via {@link #getBaseType()};
     *     </li>
     *     <li>given <code>'strict'</code> parameter value is exposed to subclasses via {@link #isStrict()} method;</li>
     *     <li>
     *          subclass is asked for {@link TypeVisitor} implementation that contains all evaluation
     *          logic ({@link #getVisitor()}). That logic is assumed to store its processing result
     *          via {@link #setMatched(boolean)} method. If that method is not called it's assumed that
     *          result is <code>false</code>;
     *     </li>
     * </ol>
     *
     * @param base              base type
     * @param candidate         candidate type
     * @param strict            flag that shows if this is a 'strict' check, e.g. if we compare <code>Integer</code>
     *                          to <code>Number</code> as a part of MyClass<Integer>
     *                          to MyClass<? extends Number> comparison, the check should be non-strict but
     *                          check of MyClass<Integer> to MyClass<Number> should be strict
     * @return                  <code>true</code> if given <code>'candidate'</code> type may be used in place
     *                          of <code>'base'</code> type; <code>false</code> otherwise
     */
    @Override
    public boolean match(T base, Type candidate, boolean strict) {
        if (base == null) {
            throw new IllegalArgumentException("Can't process AbstractTypeComplianceMatcher.match(). Reason: given "
                                               + "'base' argument is null");
        }
        if (candidate == null) {
            throw new IllegalArgumentException("Can't process AbstractTypeComplianceMatcher.match(). Reason: given "
                                               + "'candidate' argument is null");
        }
        baseType.get().push(base);
        STRICT.get().push(strict);
        try {
            typeDispatcher.dispatch(candidate, getVisitor());
            return isMatched();
        } finally {
            baseType.get().pop();
            matched.set(null);
            STRICT.get().pop();
            if (STRICT.get().size() == 1) { // We use stack size as an indicator if top-level comparison is done.
                cleanup();
            }
        }
    }

    /**
     * Allows to get type argument resolver to use.
     *
     * @return      type argument resolver to use
     */
    public TypeArgumentResolver getTypeArgumentResolver() {
        return typeArgumentResolver.get();
    }

    /**
     * Allows to define custom {@link TypeArgumentResolver} to use; {@link DefaultTypeArgumentResolver#INSTANCE}
     * is used by default.
     *
     * @param typeArgumentResolver      custom type argument resolver to use
     */
    public void setTypeArgumentResolver(TypeArgumentResolver typeArgumentResolver) {
        this.typeArgumentResolver.set(typeArgumentResolver);
    }

    /**
     * Assumed to be implemented at subclass and contain actual comparison logic.
     * <p/>
     * Check {@link #match(Type, Type, boolean)} contract for more details about how the visitor should
     * use various processing parameters and store processing result.
     *
     * @return      visitor that contains target comparison logic
     */
    protected abstract TypeVisitor getVisitor();

    /**
     * Allows to retrieve <code>'base'</code> type given to {@link #match(Type, Type, boolean)}
     * ({@link #match(Type, Type)}).
     *
     * @return      <code>'base'</code> type given to {@link #match(Type, Type, boolean)} ({@link #match(Type, Type)})
     *              if this method is called during <code>'match()'</code> method call; <code>null</code> if this
     *              method is called before or after <code>'match()'</code> call
     */
    protected T getBaseType() {
        return baseType.get().peek();
    }

    /**
     * Allows to define matching result.
     *
     * @param matched       flag that shows if types are matched
     */
    protected void setMatched(boolean matched) {
        this.matched.set(matched);
    }

    /**
     * @return      <code>'strict'</code> parameter given to {@link #match(Type, Type, boolean)} method
     */
    protected boolean isStrict() {
        return STRICT.get().peek();
    }

    /**
     * Allows to dispatch given type against given visitor.
     * <p/>
     * Follows {@link TypeDispatcher#dispatch(Type, TypeVisitor)} contract.
     *
     * @param type          type to dispatch
     * @param visitor       visitor to use during the dispatching
     */
    protected void dispatch(Type type, TypeVisitor visitor) {
        typeDispatcher.dispatch(type, visitor);
    }

    /**
     * Callback that can be used at subclasses in order to process the event of initial comparison finish.
     * <p/>
     * Default implementation (provided by this class) does nothing.
     */
    protected void cleanup() {
    }

    private boolean isMatched() {
        Boolean matched = this.matched.get();
        return matched == null ? false : matched;
    }
}