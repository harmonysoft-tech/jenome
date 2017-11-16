package org.harmony.jenome.resolve.impl;

import org.harmony.jenome.resolve.TypeVisitor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * Implements {@link TypeVisitor} with empty mehod bodies.
 */
public class TypeVisitorAdapter implements TypeVisitor {

    @Override
    public void visitParameterizedType(@NotNull ParameterizedType type) {
    }

    @Override
    public void visitWildcardType(@NotNull WildcardType type) {
    }

    @Override
    public void visitGenericArrayType(@NotNull GenericArrayType type) {
    }

    @Override
    public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
    }

    @Override
    public void visitClass(@NotNull Class<?> clazz) {
    }

    @Override
    public void visitType(@NotNull Type type) {
    }
}