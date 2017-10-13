package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * Allows to check if given type may be used in place of base {@link ParameterizedType}.
 *
 * @author Denis Zhdanov
 */
public class ParameterizedTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<ParameterizedType> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(ParameterizedType candidateType) {
            // Return eagerly if raw types don't match.
            if (!getDelegate().match(getBaseType().getRawType(), candidateType.getRawType(), isStrict())) {
                return;
            }

            Type[] baseTypeArguments = getBaseType().getActualTypeArguments();
            Type[] candidateTypeArguments = new Type[baseTypeArguments.length];

            // Resolve actual type argument types.
            for (int i = 0; i < baseTypeArguments.length; ++i) {
                if (getBaseType().getRawType() == candidateType.getRawType()) {
                    candidateTypeArguments[i] = candidateType.getActualTypeArguments()[i];
                } else {
                    candidateTypeArguments[i] = getTypeArgumentResolver().resolve(getBaseType(), candidateType, i);
                }
            }

            // Check type arguments conformance.
            for (int i = 0; i < baseTypeArguments.length; ++i) {
                // Note that we explicitly set 'strict' to 'true' here because there is no covariance
                // for type arguments in java.
                boolean result = getDelegate().match(baseTypeArguments[i], candidateTypeArguments[i], true);
                if (!result) {
                    return;
                }
            }

            if (getBaseType().getRawType() == candidateType.getRawType()) {
                setMatched(true);
                return;
            }

            // Check that pairs of corresponding arguments conform to each other (1st vs 1st, 2nd vs 2nd etc).
            // I.e. there is a possible situation that we have two parameterized types and one of them has the
            // same type argument repeated at more than one position and another has different arguments
            // (MyType<A, A> vs MyType<X, Y>). We want to consider such types to be inconsistent.
            if (!checkTypeArgumentsRepetition(baseTypeArguments, candidateTypeArguments)) {
                return;
            }

            setMatched(true);
        }

        @Override
        public void visitWildcardType(WildcardType wildcardType) {
            for (Type type : wildcardType.getUpperBounds()) {
                if (!getDelegate().match(getBaseType(), type, true)) {
                    return;
                }
            }
            setMatched(true);
        }

        @Override
        public void visitClass(Class<?> clazz) {
            if (!getDelegate().match(getBaseType().getRawType(), clazz, isStrict())) {
                return;
            }

            Type[] baseTypeArguments = getBaseType().getActualTypeArguments();
            Type[] candidateTypeArguments = new Type[baseTypeArguments.length];
            for (int i = 0; i < baseTypeArguments.length; ++i) {
                candidateTypeArguments[i] = getTypeArgumentResolver().resolve(getBaseType(), clazz, i);
                if (!getDelegate().match(baseTypeArguments[i], candidateTypeArguments[i], isStrict())) {
                    return;
                }
            }

            if (!checkTypeArgumentsRepetition(baseTypeArguments, candidateTypeArguments)) {
                return;
            }

            setMatched(true);
        }
    };

    public ParameterizedTypeComplianceMatcher() {
    }

    public ParameterizedTypeComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }

    /**
     * Allows to check is given type arrays hold the same values at the same positions, i.e. if first type
     * holds the same arguments at more than one position, candidate type arguments are the same at the same
     * positions, i.e. {@code <A, A, B>} matches to {@code <X, X, Z>} but not to {@code <X, Y, Z>}.
     *
     * @param first     first types array to check
     * @param second    second types array to check
     * @return          <code>true</code> if examination is successful; <code>false</code> otherwise
     */
    private boolean checkTypeArgumentsRepetition(Type[] first, Type[] second) {
        for (int i = 0; i < first.length; ++i) {
            for (int j = i + 1; j < first.length; ++j) {
                if (first[i] == first[j] && second[i] != second[j]) {
                    return false;
                }
            }
        }
        return true;
    }
}