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
     * <p>E.g. a {@code Comparable<Integer>} can be used in place of {@code Comparable<? extends Number>}.</p>
     *
     * @param base          base type
     * @param candidate     candidate type
     * @return              {@code true} if given {@code 'candidate'} type may be used in place
     *                      of {@code 'base'} type; {@code false} otherwise
     */
    boolean match(@NotNull T base, @NotNull Type candidate);
}