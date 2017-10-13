package org.harmony.jenome.resolve;

import java.lang.reflect.*;

/**
 * Stands for the visitor from <code>GoF Visitor</code> pattern for {@link Type} dispatching.
 *
 * @author Denis Zhdanov
 */
public interface TypeVisitor {

    void visitParameterizedType(ParameterizedType type);

    void visitWildcardType(WildcardType type);

    void visitGenericArrayType(GenericArrayType type);

    void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type);

    void visitClass(Class<?> clazz);

    void visitType(Type type);
}