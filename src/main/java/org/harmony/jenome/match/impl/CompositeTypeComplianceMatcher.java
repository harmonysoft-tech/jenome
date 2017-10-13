package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;

import java.lang.reflect.*;

/**
 * Generalizes {@link TypeComplianceMatcher} contract in order to perform double dispatch for the
 * <code>'base'</code> type in order to delegate the job to more specialized implementation.
 * <p/>
 * This class is not singleton but offers single-point-of-usage field ({@link #INSTANCE}).
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since Nov 3, 2009
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
        public void visitParameterizedType(ParameterizedType type) {
            setMatched(parameterizedTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitWildcardType(WildcardType type) {
            setMatched(wildcardTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            setMatched(genericArrayTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
            setMatched(typeVariableComplianceMatcher.match(type, getBaseType(), isStrict()));
        }

        @Override
        public void visitClass(Class<?> clazz) {
            setMatched(classComplianceMatcher.match(clazz, getBaseType(), isStrict()));
        }

        @Override
        public void visitType(Type type) {
            setMatched(topLevelTypeComplianceMatcher.match(type, getBaseType(), isStrict()));
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean match(Type base, Type candidate) throws IllegalArgumentException {
        // Overrides basic method in order to perform triple dispatch. I.e. first type dispatch is performed
        // against 'base' type in order to find corresponding TypeComplianceMatcher implementation and that
        // implementation is asked to check given 'candidate' type.
        return super.match(candidate, base, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean match(Type base, Type candidate, boolean topLevelCheck) {
        // Overrides basic method in order to perform triple dispatch. I.e. first type dispatch is performed
        // against 'base' type in order to find corresponding TypeComplianceMatcher implementation and that
        // implementation is asked to check given 'candidate' type.
        return super.match(candidate, base, topLevelCheck);
    }

    /** {@inheritDoc} */
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}
