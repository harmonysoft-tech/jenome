package tech.harmonysoft.oss.jenome.match.impl;

import tech.harmonysoft.oss.jenome.match.TypeComplianceMatcher;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

public class TypeVariableComplianceMatcher
        extends AbstractDelegatingTypeComplianceMatcher<TypeVariable<? extends GenericDeclaration>>
{

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            checkBounds(type);
        }

        @Override
        public void visitWildcardType(@NotNull WildcardType type) {
            checkBounds(type);
        }

        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            checkBounds(type);
        }

        @Override
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
            for (Type baseBound : getBaseType().getBounds()) {
                if (baseBound == Object.class) {
                    // java.lang.Object as a type variable bound means that type is actually inbound, so, we just
                    // skip it here.
                    continue;
                }

                // We assume that base type variable bound restriction is satisfied if base type bound is matched
                // at least to one candidate type variable bound.
                boolean matched = false;
                for (Type candidateBound : type.getBounds()) {
                    if (candidateBound != Object.class && getDelegate().match(baseBound, candidateBound)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return;
                }
            }
            setMatched(true);
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            checkBounds(clazz);
        }
    };

    public TypeVariableComplianceMatcher() {
    }

    public TypeVariableComplianceMatcher(@NotNull TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }

    private void checkBounds(@NotNull Type type) {
        for (Type boundType : getBaseType().getBounds()) {
            // java.lang.Object as a bound type means that type is actually inbound, so, we just skip it here.
            if (boundType != Object.class && !getDelegate().match(boundType, type)) {
                return;
            }
        }
        setMatched(true);
    }
}