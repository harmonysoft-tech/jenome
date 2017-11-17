package tech.harmonysoft.oss.jenome.match.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.jenome.match.TypeComplianceMatcher;
import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.DefaultTypeArgumentResolver;
import tech.harmonysoft.oss.jenome.resolve.util.TypeDispatcher;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 *      {@link TypeComplianceMatcher} implementation that is based on GoF {@code 'Template Method'} pattern.
 * </p>
 * <p>
 *      I.e. this class defines general algorithm, offers useful facilities for subclasses and requires
 *      them to implement particular functionality.
 * </p>
 * <p>Thread-safe.</p>
 *
 * @param <T>   target {@code 'base'} type
 * @see #match(Type, Type, boolean)
 */
public abstract class AbstractTypeComplianceMatcher<T extends Type> implements TypeComplianceMatcher<T> {

    /**
     * <p>
     *      Holds stack of flags that indicate if {@code 'strict'} check is performed
     *      (check {@link #match(Type, Type, boolean)}) contract for more details.
     * </p>
     * <p>
     *      We use static variable here in order to be able to keep track of {@code 'strict'} value across
     *      multiple instances of underlying classes.
     * </p>
     */
    private static final ThreadLocal<Stack<Boolean>> STRICT = new ThreadLocal<>();
    static {
        Stack<Boolean> stack = new Stack<>();
        stack.push(false);
        STRICT.set(stack);
    }

    /**
     * <p>
     *      Stores {@code 'base'} type used in comparison. That type is available to actual implementations
     *      via {@link #getBaseType()} method.
     * </p>
     * <p>
     *      We use stack of values here in order to be able to handle the situation when the same matcher
     *      implementation is used more than one during the same type comparison. Example of such a situation
     *      is comparison of {@code Comparable<Collection<Comparable<? extends Number>>>} vs
     *      {@code Comparable<Collection<Comparable<Long>>>}. Matcher that works with {@link ParameterizedType}
     *      is used for different types here ({@link Comparable} and {@link Collection}), so, we need to keep
     *      track of base type between those comparisons.
     * </p>
     */
    private final ThreadLocal<Stack<T>>                 baseType             = ThreadLocal.withInitial(Stack::new);
    private final ThreadLocal<Boolean>                  matched              = new ThreadLocal<>();
    private final AtomicReference<TypeArgumentResolver> typeArgumentResolver = new AtomicReference<>();
    private final TypeDispatcher                        typeDispatcher       = new TypeDispatcher();

    protected AbstractTypeComplianceMatcher() {
        matched.set(false);
        setTypeArgumentResolver(DefaultTypeArgumentResolver.INSTANCE);
    }

    @Override
    public boolean match(@NotNull T base, @NotNull Type candidate) {
        return match(base, candidate, false);
    }

    /**
     * Template method that defines basic match algorithm:
     * <ol>
     *     <li>
     *          given {@code 'base'} type is remembered at thread-local variable and is available
     *          to subclasses via {@link #getBaseType()};
     *     </li>
     *     <li>given {@code 'strict'} parameter value is exposed to subclasses via {@link #isStrict()} method;</li>
     *     <li>
     *          subclass is asked for {@link TypeVisitor} implementation that contains all evaluation
     *          logic ({@link #getVisitor()}). That logic is assumed to store its processing result
     *          via {@link #setMatched(boolean)} method. If that method is not called it's assumed that
     *          result is {@code false};
     *     </li>
     * </ol>
     *
     * @param base              base type
     * @param candidate         candidate type
     * @param strict            flag that indicates if a 'strict' check should be used, e.g. if we compare
     *                          {@code Integer} to {@code Number} as a part of {@code MyClass<Integer>}
     *                          to {@code MyClass<? extends Number>} comparison, the check should be non-strict but
     *                          check for {@code MyClass<Integer>} to {@code MyClass<Number>} should be strict
     * @return                  {@code true} if given {@code 'candidate'} type may be used in place
     *                          of {@code 'base'} type; {@code false} otherwise
     */
    public boolean match(@NotNull T base, @NotNull Type candidate, boolean strict) {
        baseType.get().push(base);
        STRICT.get().push(strict);
        try {
            typeDispatcher.dispatch(candidate, getVisitor());
            return isMatched();
        } finally {
            baseType.get().pop();
            matched.set(null);
            STRICT.get().pop();
        }
    }

    /**
     * Allows to get type argument resolver to use.
     *
     * @return      type argument resolver to use
     */
    @NotNull
    public TypeArgumentResolver getTypeArgumentResolver() {
        return typeArgumentResolver.get();
    }

    /**
     * Allows to define custom {@link TypeArgumentResolver} to use; {@link DefaultTypeArgumentResolver#INSTANCE}
     * is used by default.
     *
     * @param typeArgumentResolver      custom type argument resolver to use
     */
    public void setTypeArgumentResolver(@NotNull TypeArgumentResolver typeArgumentResolver) {
        this.typeArgumentResolver.set(typeArgumentResolver);
    }

    /**
     * <p>Assumed to be implemented at subclass and contain actual comparison logic.</p>
     * <p>
     *      Check {@link #match(Type, Type, boolean)} contract for more details about how the visitor should
     *      use various processing parameters and store processing result.
     * </p>
     *
     * @return      visitor that contains target comparison logic
     */
    @NotNull
    protected abstract TypeVisitor getVisitor();

    /**
     * Allows to retrieve {@code 'base'} type given to {@link #match(Type, Type, boolean)}
     * ({@link #match(Type, Type)}).
     *
     * @return      {@code 'base'} type given to {@link #match(Type, Type, boolean)} ({@link #match(Type, Type)})
     *              if this method is called during {@code 'match()'} method call; {@code null} if this
     *              method is called before or after {@code 'match()'} call
     */
    @NotNull
    protected T getBaseType() {
        T result = baseType.get().peek();
        if (result == null) {
            throw new IllegalStateException("Can't find a base type. Make sure that the call is performed during "
                                            + "match() processing");
        }
        return result;
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
     * @return      {@code 'strict'} parameter given to {@link #match(Type, Type, boolean)} method
     */
    protected boolean isStrict() {
        return STRICT.get().peek();
    }

    /**
     * <p>Allows to dispatch given type against given visitor.</p>
     * <p>Follows {@link TypeDispatcher#dispatch(Type, TypeVisitor)} contract.</p>
     *
     * @param type          type to dispatch
     * @param visitor       visitor to use during the dispatching
     */
    protected void dispatch(@NotNull Type type, @NotNull TypeVisitor visitor) {
        typeDispatcher.dispatch(type, visitor);
    }

    private boolean isMatched() {
        Boolean matched = this.matched.get();
        return matched == Boolean.TRUE;
    }
}