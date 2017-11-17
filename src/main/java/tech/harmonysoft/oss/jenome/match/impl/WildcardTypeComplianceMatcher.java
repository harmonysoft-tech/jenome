package tech.harmonysoft.oss.jenome.match.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * Allows to check if given type may be used in place of base {@link WildcardType}.
 */
public class WildcardTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<WildcardType> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
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
        public void visitWildcardType(@NotNull WildcardType type) {
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
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
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
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
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
        public void visitClass(@NotNull Class<?> clazz) {
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
        public void visitParameterizedType(@NotNull ParameterizedType type) {
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
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            genericArrayHolder.set(type);
        }
    };

    private final ThreadLocal<ParameterizedType> parameterizedTypeHolder = new ThreadLocal<>();
    private final ThreadLocal<GenericArrayType> genericArrayHolder = new ThreadLocal<>();

    public WildcardTypeComplianceMatcher() {
    }

    public WildcardTypeComplianceMatcher(@NotNull AbstractTypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
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
     * <p>
     *      Allows to check if lower bounds (if any) of the {@link #getBaseType() base wildcard type} prevent given
     *      type to be used in place of it.
     * </p>
     * <p>It's assumed that given type is not a wildcard type.</p>
     *
     * @param type      type to check against {@link #getBaseType() base wildcard type} lower bounds
     * @return          {@code true} if lower bounds of the {@link #getBaseType() base wildcard type}
     *                  don't prevent given type to be used in place of it; {@code false} otherwise
     */
    private boolean checkBaseLowerBounds(@NotNull Type type) {
        for (Type boundType : getBaseType().getLowerBounds()) {
            if (!getDelegate().match(type, boundType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     *      This method contains the logic for special case comparison - checking if lower bounds may prevent one
     *      wildcard type may be used in place of another wildcard type.
     * </p>
     * <p>
     *     I.e. this method is intended to handle lower-bound comparisons like
     *     {@code ? super List<? super Set<Intget>>'} vs {@code ? super Collection<? super Collection<Integer>>}
     * </p>
     *
     * @param type      candidate wildcard type
     * @return          {@code true} if wildcard lower bound don't prevent given wildcard type to be used
     *                  in place of the {@link #getBaseType() base wildcard type}; {@code false} otherwise
     */
    private boolean checkWildcardCandidateLowerBounds(@NotNull WildcardType type) {
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
     * <p>
     *      Follows the contract of {@link #checkWildcardCandidateLowerBounds(WildcardType)} for the special case
     *      when two lower bounds are {@link ParameterizedType} instances.
     * </p>
     * <p>
     *      The general idea is to correctly perform checking for the comparisons
     *      like {@code ? super List<? super Long>>} vs {@code ? super Collection<? super Number>}.
     *      We need to check that {@code List IS-A Collection} and that {@code Long IS-A Number} here.
     * </p>
     *
     * @param baseLowerBound            {@code 'base'} type lower bound
     * @param candidateLowerBound       {@code 'candidate'} type lower bound
     * @return                          {@code true} if given lower bounds are {@link ParameterizedType} and
     *                                  {@code 'candidate'} lower bound usage doesn't contradict to
     *                                  {@code 'base'} lower bound usage; {@code false} if both given
     *                                  arguments are {@link ParameterizedType} and {@code 'candidate'} bound
     *                                  contradicts to {@code 'base'} bound; {@code null} if any of the
     *                                  given types is not {@link ParameterizedType}
     *
     */
    private Boolean checkParameterizedTypeSpecialCase(@NotNull Type baseLowerBound,
                                                      @NotNull Type candidateLowerBound)
    {
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
     * <p>
     *      Follows the contract of {@link #checkWildcardCandidateLowerBounds(WildcardType)} for the special case
     *      when two lower bounds are {@link ParameterizedType} instances.
     * </p>
     * <p>
     *     Just delegates to {@link #checkParameterizedTypeSpecialCase(Type, Type)} for the generic arrays
     *     component types.
     * </p>
     *
     * @param baseLowerBound          {@code 'base'} type lower bound
     * @param candidateLowerBound     {@code 'candidate'} type lower bound
     * @return                        {@code true} if given lower bounds are {@link GenericArrayType} and
     *                                {@code 'candidate'} lower bound usage doesn't contradict to
     *                                {@code 'base'} lower bound usage; {@code false} if both given
     *                                arguments are {@link GenericArrayType} and {@code 'candidate'} bound
     *                                contradicts to {@code 'base'} bound; {@code null} if any of the
     *                                given types is not {@link GenericArrayType}
     */
    private Boolean checkGenericArraySpecialCase(@NotNull Type baseLowerBound, @NotNull Type candidateLowerBound) {
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