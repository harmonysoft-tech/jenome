package tech.harmonysoft.oss.jenome.resolve.util;

import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * <p>Allows to dispatch object reference of static type {@link Type} to particular subtype if any.</p>
 * <p>This class is not singleton but offers single-point-of-usage field {@link #INSTANCE}.</p>
 * <p>Thread-safe.</p>
 */
public class TypeDispatcher {

    /** 'Single-point-of-usage' field. */
    public static final TypeDispatcher INSTANCE = new TypeDispatcher();

    /**
     * <p>Allows to dispatch given {@code 'type'} to the actual type to the given visitor.</p>
     * <p>
     *     Note: there is a theoretical possibility that given {@code 'type'} reference corresponds
     *     to more than one target type defined at {@link TypeVisitor} (e.g. it might implement
     *     {@link ParameterizedType} and {@link WildcardType} interfaces). All corresponding
     *     methods are called then (their order is undefined).
     * </p>
     * <p>
     *      The only exception to the rules described above is a {@link TypeVisitor#visitType(Type)} - it's called
     *      <b>only</b> when given {@code 'type'} object doesn't implement any interested {@link Type}
     *      sub-interface and it's not IS-A {@link Class}.
     * </p>
     * <p>Thread-safe.</p>
     *
     * @param type      target {@link Type} object to dispatch
     * @param visitor   visitor to use for type dispatching
     */
    public void dispatch(@NotNull Type type, @NotNull TypeVisitor visitor) {
        boolean matched = false;

        if (type instanceof ParameterizedType) {
            visitor.visitParameterizedType((ParameterizedType)type);
            matched = true;
        }

        if (type instanceof WildcardType) {
            visitor.visitWildcardType((WildcardType)type);
            matched = true;
        }

        if (type instanceof GenericArrayType) {
            visitor.visitGenericArrayType((GenericArrayType)type);
            matched = true;
        }

        if (type instanceof TypeVariable) {
            visitor.visitTypeVariable((TypeVariable<?>)type);
            matched = true;
        }

        if (type instanceof Class) {
            visitor.visitClass((Class<?>)type);
            matched = true;
        }

        if (!matched) {
            visitor.visitType(type);
        }
    }
}