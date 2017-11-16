package tech.harmonysoft.oss.jenome.match;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * <p>Defines general contract for checking if particular type may be used in place of another type.</p>
 * <p>Implementations of this interface are assumed to be thread-safe.</p>
 *
 * @author Denis Zhdanov
 * @param <T>   target {@code 'base'} type
 */
public interface TypeComplianceMatcher<T extends Type> {

    /**
     * <p>Allows to check if {@code 'candidate'} type may be used in place of {@code 'base'} type.</p>
     * <p>
     *      This method is essentially the same as {@link #match(Type, Type, boolean)} called with {@code 'false'}
     *      as last argument
     * </p>
     *
     * @param base          base type
     * @param candidate     candidate type
     * @return              {@code true} if given {@code 'candidate'} type may be used in place
     *                      of {@code 'base'} type; {@code false} otherwise
     */
    boolean match(@NotNull T base, @NotNull Type candidate);

    /**
     * Allows to check if {@code 'candidate'} type may be used in place of {@code 'base'} type.
     *
     * @param base              base type
     * @param candidate         candidate type
     * @param strict            flag that shows if this is a 'strict' check, e.g. if we compare {@code Integer}
     *                          to {@code Number} as a part of MyClass&lt;Integer&gt;
     *                          to MyClass&lt;? extends Number&lt; comparison, the check should be non-strict but
     *                          check of MyClass&lt;Integer&gt; to MyClass&lt;Number&gt; should be strict
     * @return                  {@code true} if given {@code 'candidate'} type may be used in place
     *                          of {@code 'base'} type; {@code false} otherwise
     */
    boolean match(@NotNull T base, @NotNull Type candidate, boolean strict);
}