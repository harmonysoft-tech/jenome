package tech.harmonysoft.oss.jenome.match.impl;

import tech.harmonysoft.oss.jenome.match.TypeComplianceMatcher;
import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
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