package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * Allows to check if particular class may be matched to particular type.
 * <p/>
 * Provides actual functionality described at the contract of {@link GenericsHelper#match(Type, Class)} method.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class ClassComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<Class<?>> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(ParameterizedType type) {
            setMatched(getDelegate().match(getBaseType(), type.getRawType()));
        }

        @Override
        public void visitWildcardType(WildcardType type) {
            for (Type upperBoundType : type.getUpperBounds()) {
                if (!getDelegate().match(getBaseType(), upperBoundType, true)) {
                    return;
                }
            }

            setMatched(type.getLowerBounds().length <= 0 || getBaseType() == Object.class);
        }

        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            if (!getBaseType().isArray()) {
                setMatched(false);
                return;
            }

            setMatched(getDelegate().match(getBaseType().getComponentType(), type.getGenericComponentType(), true));
        }

        @Override
        public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
            // We know that java.lang.Object is returned if no upper bound is defined explicitly.
            for (Type upperBoundType : type.getBounds()) {
                if (!getDelegate().match(getBaseType(), upperBoundType)) {
                    return;
                }
            }
            setMatched(true);
        }

        @Override
        public void visitClass(Class<?> clazz) {
            boolean matched = isStrict() ? getBaseType() == clazz : getBaseType().isAssignableFrom(clazz);
            setMatched(matched);
        }
    };

    public ClassComplianceMatcher() {
    }

    public ClassComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}