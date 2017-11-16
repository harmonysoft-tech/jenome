package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * <p>
 *      Generalizes {@link TypeComplianceMatcher} contract in order to perform double dispatch for the
 *      {@code 'base'} type in order to delegate the job to more specialized implementation.
 * </p>
 * <p>This class is not singleton but offers single-point-of-usage field ({@link #INSTANCE}).</p>
 * <p>Thread-safe.</p>
 */
public class CompositeTypeComplianceMatcher extends AbstractTypeComplianceMatcher<Type> {

    /** Single-point-of-usage field. */
    public static final CompositeTypeComplianceMatcher INSTANCE = new CompositeTypeComplianceMatcher();

    private final ClassComplianceMatcher classComplianceMatcher = new ClassComplianceMatcher(this);
    private final WildcardTypeComplianceMatcher wildcardTypeComplianceMatcher
                                                                = new WildcardTypeComplianceMatcher(this);
    private final ParameterizedTypeComplianceMatcher parameterizedTypeComplianceMatcher
                                                                = new ParameterizedTypeComplianceMatcher(this);
    private final GenericArrayTypeComplianceMatcher genericArrayTypeComplianceMatcher
                                                                = new GenericArrayTypeComplianceMatcher(this);
    private final TypeVariableComplianceMatcher typeVariableComplianceMatcher = new TypeVariableComplianceMatcher(this);
    private final TopLevelTypeComplianceMatcher topLevelTypeComplianceMatcher = new TopLevelTypeComplianceMatcher(this);

    private final TypeVisitor visitor = new TypeVisitor() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            setMatched(parameterizedTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitWildcardType(@NotNull WildcardType type) {
            setMatched(wildcardTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            setMatched(genericArrayTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
            setMatched(typeVariableComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            setMatched(classComplianceMatcher.match(clazz, getBaseType(), isStrict()));
        }

        @Override
        public void visitType(@NotNull Type type) {
            setMatched(topLevelTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }
    };

    @Override
    public boolean match(@NotNull Type base, @NotNull Type candidate) throws IllegalArgumentException {
        // Overrides basic method in order to perform triple dispatch. I.e. first type dispatch is performed
        // against 'base' type in order to find corresponding TypeComplianceMatcher implementation and that
        // implementation is asked to check given 'candidate' type.
        return super.match(candidate, base, false);
    }

    @Override
    public boolean match(@NotNull Type base, @NotNull Type candidate, boolean topLevelCheck) {
        // Overrides basic method in order to perform triple dispatch. I.e. first type dispatch is performed
        // against 'base' type in order to find corresponding TypeComplianceMatcher implementation and that
        // implementation is asked to check given 'candidate' type.
        return super.match(candidate, base, topLevelCheck);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}
