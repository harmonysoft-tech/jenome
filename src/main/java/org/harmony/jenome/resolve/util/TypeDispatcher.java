package org.harmony.jenome.resolve.util;

import org.harmony.jenome.resolve.TypeVisitor;

import java.lang.reflect.*;

/**
 * Allows to dispatch object reference of static type {@link Type} to particular subtype if any.
 * <p/>
 * This class is not singleton but offers single=point-of-usage field {@link #INSTANCE}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class TypeDispatcher {

    /** 'Single-point-of-usage' field. */
    public static final TypeDispatcher INSTANCE = new TypeDispatcher();

    /**
     * Allows to dispatch given <code>'type'</code> to the actual type to the given visitor.
     * <p/>
     * <b>Note:<b> there is a theoretical possibility that given <code>'type'</code> reference corresponds
     * to more than one target type defined at {@link TypeVisitor} (e.g. it may implement {@link ParameterizedType}
     * and {@link WildcardType} interfaces). All corresponding <code>'visitParameterizedType()'</code> methods are called then
     * (their order is undefined).
     * <p/>
     * The only exception to the rules described above is a {@link TypeVisitor#visitType(Type)} - it's called
     * <b>only</b> when given <code>'type'</code> object doesn't implement any interested {@link Type}
     * sub-interface and it's not IS-A {@link Class}.
     * <p/>
     * Thread-safe.
     *
     * @param type      target {@link Type} object to dispatch
     * @param visitor   visitor to use for type dispatching
     * @throws IllegalArgumentException     if any of the given arguments is <code>null</code>
     */
    public void dispatch(Type type, TypeVisitor visitor) throws IllegalArgumentException {
        if (type == null) {
            throw new IllegalArgumentException("Can't process TypeDispatcher.dispatch(). Reason: given 'type' "
                                               + "argument is null");
        }
        if (visitor == null) {
            throw new IllegalArgumentException("Can't process TypeDispatcher.dispatch(). Reason: given 'visitor' "
                                               + "argument is null");
        }

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