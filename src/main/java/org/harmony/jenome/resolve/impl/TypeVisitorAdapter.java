package org.harmony.jenome.resolve.impl;

import org.harmony.jenome.resolve.TypeVisitor;

import java.lang.reflect.*;

/**
 * Implements {@link TypeVisitor} with empty mehod bodies.
 *
 * @author Denis Zhdanov
 */
public class TypeVisitorAdapter implements TypeVisitor {

    @Override
    public void visitParameterizedType(ParameterizedType type) {
    }

    @Override
    public void visitWildcardType(WildcardType type) {
    }

    @Override
    public void visitGenericArrayType(GenericArrayType type) {
    }

    @Override
    public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
    }

    @Override
    public void visitClass(Class<?> clazz) {
    }

    @Override
    public void visitType(Type type) {
    }
}