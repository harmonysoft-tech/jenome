package org.harmony.jenome.match;

import java.lang.reflect.Type;

/**
 * Defines general contract for checking if particular type may be used in place of another type.
 * <p/>
 * Implementations of this interface are assumed to be thread-safe.
 *
 * @author Denis Zhdanov
 * @param <T>   target <code>'base'</code> type
 */
public interface TypeComplianceMatcher<T extends Type> {

    /**
     * Allows to check if <code>'candidate'</code> type may be used in place of <code>'base'</code> type.
     * <p/>
     * This method is essentially the same as {@link #match(Type, Type, boolean)} called with <code>'false'</code>
     * as last argument
     *
     * @param base          base type
     * @param candidate     candidate type
     * @return              <code>true</code> if given <code>'candidate'</code> type may be used in place
     *                      of <code>'base'</code> type; <code>false</code> otherwise
     */
    boolean match(T base, Type candidate);

    /**
     * Allows to check if <code>'candidate'</code> type may be used in place of <code>'base'</code> type.
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
    boolean match(T base, Type candidate, boolean strict);
}