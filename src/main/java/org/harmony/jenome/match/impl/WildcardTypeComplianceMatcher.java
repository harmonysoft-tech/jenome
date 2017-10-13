package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * Allows to check if given type may be used in place of base {@link WildcardType}.
 *
 * @author Denis Zhdanov
 * @since Nov 4, 2009
 */
public class WildcardTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<WildcardType> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(ParameterizedType type) {
            if (isUnboundWildcard()) {
                return;
            }
            for (Type upperBound : getBaseType().getUpperBounds()) {
                if (!getDelegate().match(upperBound, type)) {
                    return;
                }
            }
            setMatched(checkBaseLowerBounds(type));
        }

        @Override
        public void visitWildcardType(WildcardType type) {
            if (isUnboundWildcard()) {
                return;
            }
            Type[] baseUpperBounds = getBaseType().getUpperBounds();
            Type[] candidateUpperBounds = type.getUpperBounds();

            for (Type baseUpperBound : baseUpperBounds) {
                boolean matched = false;
                for (Type candidateUpperBound : candidateUpperBounds) {
                    if (getDelegate().match(baseUpperBound, candidateUpperBound)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return;
                }
            }

            Type[] baseLowerBounds = getBaseType().getLowerBounds();

            // We assume here that the match is always failed if base type has lower bounds and candidate type
            // has upper bound.
            if (baseLowerBounds.length > 0
                && (candidateUpperBounds.length > 1 || candidateUpperBounds[0] != Object.class))
            {
                return;
            }
            setMatched(checkWildcardCandidateLowerBounds(type));
        }

        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            if (isUnboundWildcard()) {
                return;
            }
            for (Type upperBound : getBaseType().getUpperBounds()) {
                if (!getDelegate().match(upperBound, type)) {
                    return;
                }
            }
            setMatched(checkBaseLowerBounds(type));
        }

        @Override
        public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
            if (isUnboundWildcard()) {
                return;
            }

            if (getBaseType().getLowerBounds().length > 0) {
                return;
            }

            for (Type upperBound : getBaseType().getUpperBounds()) {
                boolean matched = false;
                for (Type typeVariableBound : type.getBounds()) {
                    if (typeVariableBound == Object.class) {
                        continue;
                    }
                    matched = getDelegate().match(upperBound, typeVariableBound);
                    if (!matched) {
                        return;
                    }
                }
                if (!matched) {
                    return;
                }
            }
            setMatched(checkBaseLowerBounds(type));
        }

        @Override
        public void visitClass(Class<?> clazz) {
            if (isUnboundWildcard()) {
                return;
            }
            for (Type type : getBaseType().getUpperBounds()) {
                if (!getDelegate().match(type, clazz)) {
                    return;
                }
            }
            setMatched(checkBaseLowerBounds(clazz));
        }
    };

    /**
     * This visitor checks if dispatched type is {@link ParameterizedType} and stored it at the
     * {@link #parameterizedTypeHolder} in the case of success.
     *
     * @see #checkParameterizedTypeSpecialCase(Type, Type)
     */
    private final TypeVisitor parameterizedTypeRetriever = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(ParameterizedType type) {
            parameterizedTypeHolder.set(type);
        }
    };

    /**
     * This visitor checks if dispatched type is {@link GenericArrayType} and stored it at the
     * {@link #genericArrayHolder} in the case of success.
     *
     * @see #checkGenericArraySpecialCase(Type, Type)
     */
    private final TypeVisitor genericArrayRetriever = new TypeVisitorAdapter() {
        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            genericArrayHolder.set(type);
        }
    };

    private final ThreadLocal<ParameterizedType> parameterizedTypeHolder = new ThreadLocal<ParameterizedType>();
    private final ThreadLocal<GenericArrayType> genericArrayHolder = new ThreadLocal<GenericArrayType>();

    public WildcardTypeComplianceMatcher() {
    }

    public WildcardTypeComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }

    private boolean isUnboundWildcard() {
        Type[] lowerBounds = getBaseType().getLowerBounds();
        Type[] upperBounds = getBaseType().getUpperBounds();
        return lowerBounds.length == 0 && upperBounds.length == 1 && getBaseType().getUpperBounds()[0] == Object.class;
    }

    /**
     * Allows to check if lower bounds (if any) of the {@link #getBaseType() base wildcard type} prevent given
     * type to be used in place of it.
     * <p/>
     * It's assumed that given type is not a wildcard type.
     *
     * @param type      type to check against {@link #getBaseType() base wildcard type} lower bounds
     * @return          <code>true</code> if lower bounds of the {@link #getBaseType() base wildcard type}
     *                  don't prevent given type to be used in place of it; <code>false</code> otherwise
     */
    private boolean checkBaseLowerBounds(Type type) {
        for (Type boundType : getBaseType().getLowerBounds()) {
            if (!getDelegate().match(type, boundType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method contains the logic for special case comparison - checking if lower bounds may prevent one
     * wildcard type may be used in place of another wildcard type.
     * <p/>
     * I.e. this method is intended to handle lower-bound comparisons like
     * {@code ? super List<? super Set<Intget>>'} vs {@code ? super Collection<? super Collection<Integer>>}
     *
     * @param type      candidate wildcard type
     * @return          <code>true</code> if wildcard lower bound don't prevent given wildcard type to be used
     *                  in place of the {@link #getBaseType() base wildcard type}; <code>false</code> otherwise
     */
    private boolean checkWildcardCandidateLowerBounds(WildcardType type) {
        for (Type baseLowerBound : getBaseType().getLowerBounds()) {
            for (Type candidateLowerBound : type.getLowerBounds()) {
                Boolean specialCaseResult;
                try {
                    specialCaseResult = checkParameterizedTypeSpecialCase(baseLowerBound, candidateLowerBound);
                    if (specialCaseResult == null) {
                        specialCaseResult = checkGenericArraySpecialCase(baseLowerBound, candidateLowerBound);
                    }
                } finally {
                    parameterizedTypeHolder.set(null);
                    genericArrayHolder.set(null);
                }
                boolean matched;
                if (specialCaseResult == null) {
                    matched = getDelegate().match(candidateLowerBound, baseLowerBound);
                } else {
                    matched = specialCaseResult;
                }
                if (!matched) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Follows the contract of {@link #checkWildcardCandidateLowerBounds(WildcardType)} for the special case
     * when two lower bounds are {@link ParameterizedType} instances.
     * <p/>
     * The general idea is to correctly perform checking for the comparisons like {@code ? super List<? super Long>>}
     * vs {@code ? super Collection<? super Number>}. We need to check that <code>List IS-A Collection</code>
     * and that <code>Long IS-A Number</code> here.
     *
     * @param baseLowerBound            <code>'base'</code> type lower bound
     * @param candidateLowerBound       <code>'candidate'</code> type lower bound
     * @return                          <code>true</code> if given lower bounds are {@link ParameterizedType} and
     *                                  <code>'candidate'</code> lower bound usage doesn't contradict to
     *                                  <code>'base'</code> lower bound usage; <code>false</code> if both given
     *                                  arguments are {@link ParameterizedType} and <code>'candidate'</code> bound
     *                                  contradicts to <code>'base'</code> bound; <code>null</code> if any of the
     *                                  given types is not {@link ParameterizedType}
     *
     */
    private Boolean checkParameterizedTypeSpecialCase(Type baseLowerBound, Type candidateLowerBound) {
        dispatch(baseLowerBound, parameterizedTypeRetriever);
        ParameterizedType baseType = parameterizedTypeHolder.get();
        if (baseType == null) {
            return null;
        }

        parameterizedTypeHolder.set(null);
        dispatch(candidateLowerBound, parameterizedTypeRetriever);
        ParameterizedType candidateType = parameterizedTypeHolder.get();
        if (candidateType == null) {
            return null;
        }

        if (!getDelegate().match(candidateType.getRawType(), baseType.getRawType())) {
            return false;
        }

        Type[] candidateArguments = candidateType.getActualTypeArguments();
        for (int i = 0; i < candidateArguments.length; ++i) {
            Type baseArgument = getTypeArgumentResolver().resolve(candidateType, baseType, i);
            if (!getDelegate().match(baseArgument, candidateArguments[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Follows the contract of {@link #checkWildcardCandidateLowerBounds(WildcardType)} for the special case
     * when two lower bounds are {@link ParameterizedType} instances.
     * <p/>
     * Just delegates to {@link #checkParameterizedTypeSpecialCase(Type, Type)} for the generic arrays component types.
     *
     * @param baseLowerBound          <code>'base'</code> type lower bound
     * @param candidateLowerBound     <code>'candidate'</code> type lower bound
     * @return                        <code>true</code> if given lower bounds are {@link GenericArrayType} and
     *                                <code>'candidate'</code> lower bound usage doesn't contradict to
     *                                <code>'base'</code> lower bound usage; <code>false</code> if both given
     *                                arguments are {@link GenericArrayType} and <code>'candidate'</code> bound
     *                                contradicts to <code>'base'</code> bound; <code>null</code> if any of the
     *                                given types is not {@link GenericArrayType}
     */
    private Boolean checkGenericArraySpecialCase(Type baseLowerBound, Type candidateLowerBound) {
        dispatch(baseLowerBound, genericArrayRetriever);
        GenericArrayType baseType = genericArrayHolder.get();
        if (baseType == null) {
            return null;
        }

        genericArrayHolder.set(null);
        dispatch(candidateLowerBound, genericArrayRetriever);
        GenericArrayType candidateType = genericArrayHolder.get();
        if (candidateType == null) {
            return null;
        }

        return checkParameterizedTypeSpecialCase(
                baseType.getGenericComponentType(), candidateType.getGenericComponentType()
        );
    }
}