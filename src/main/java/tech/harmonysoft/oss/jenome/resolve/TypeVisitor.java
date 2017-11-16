package tech.harmonysoft.oss.jenome.resolve;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * A {@code GoF Visitor} for {@link Type} dispatching.
 */
public interface TypeVisitor {

    void visitParameterizedType(@NotNull ParameterizedType type);

    void visitWildcardType(@NotNull WildcardType type);

    void visitGenericArrayType(@NotNull GenericArrayType type);

    void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type);

    void visitClass(@NotNull Class<?> clazz);

    void visitType(@NotNull Type type);
}