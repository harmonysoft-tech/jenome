package tech.harmonysoft.oss.jenome.match.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

public class ClassComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<Class<?>> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            setMatched(getDelegate().match(getBaseType(), type.getRawType()));
        }

        @Override
        public void visitWildcardType(@NotNull WildcardType type) {
            for (Type upperBoundType : type.getUpperBounds()) {
                if (!getDelegate().match(getBaseType(), upperBoundType, true)) {
                    return;
                }
            }

            setMatched(type.getLowerBounds().length <= 0 || getBaseType() == Object.class);
        }

        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            if (!getBaseType().isArray()) {
                setMatched(false);
                return;
            }

            setMatched(getDelegate().match(getBaseType().getComponentType(), type.getGenericComponentType(), true));
        }

        @Override
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
            // We know that java.lang.Object is returned if no upper bound is defined explicitly.
            for (Type upperBoundType : type.getBounds()) {
                if (!getDelegate().match(getBaseType(), upperBoundType)) {
                    return;
                }
            }
            setMatched(true);
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            boolean matched = isStrict() ? getBaseType() == clazz : getBaseType().isAssignableFrom(clazz);
            setMatched(matched);
        }
    };

    public ClassComplianceMatcher() {
    }

    public ClassComplianceMatcher(@NotNull AbstractTypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}