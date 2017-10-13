package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeArgumentResolver;
import org.harmony.jenome.resolve.TypeVisitor;

import java.lang.reflect.*;

/**
 * @author Denis Zhdanov
 * @since Nov 20, 2009
 */
public class TopLevelTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<Type> {

    private final TypeVisitor visitor = new TypeVisitor() {
        @Override
        public void visitParameterizedType(ParameterizedType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitWildcardType(WildcardType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitClass(Class<?> clazz) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitType(Type type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }
    };

    public TopLevelTypeComplianceMatcher() {
    }

    public TopLevelTypeComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}