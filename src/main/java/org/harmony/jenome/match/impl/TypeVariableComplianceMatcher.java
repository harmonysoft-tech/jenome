package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * @author Denis Zhdanov
 * @since Nov 10, 2009
 */
public class TypeVariableComplianceMatcher
        extends AbstractDelegatingTypeComplianceMatcher<TypeVariable<? extends GenericDeclaration>>
{

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(ParameterizedType type) {
            checkBounds(type);
        }

        @Override
        public void visitWildcardType(WildcardType type) {
            checkBounds(type);
        }

        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            checkBounds(type);
        }

        @Override
        public void visitTypeVariable(TypeVariable<? extends GenericDeclaration> type) {
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
        public void visitClass(Class<?> clazz) {
            checkBounds(clazz);
        }
    };

    public TypeVariableComplianceMatcher() {
    }

    public TypeVariableComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }

    private void checkBounds(Type type) {
        for (Type boundType : getBaseType().getBounds()) {
            // java.lang.Object as a bound type means that type is actually inbound, so, we just skip it here.
            if (boundType != Object.class && !getDelegate().match(boundType, type)) {
                return;
            }
        }
        setMatched(true);
    }
}