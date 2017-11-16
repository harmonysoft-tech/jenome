package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeArgumentResolver;
import org.harmony.jenome.resolve.TypeVisitor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

public class TopLevelTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<Type> {

    private final TypeVisitor visitor = new TypeVisitor() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitWildcardType(@NotNull WildcardType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }

        @Override
        public void visitType(@NotNull Type type) {
            setMatched(getBaseType() == TypeArgumentResolver.RAW_TYPE);
        }
    };

    public TopLevelTypeComplianceMatcher() {
    }

    public TopLevelTypeComplianceMatcher(@NotNull TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}