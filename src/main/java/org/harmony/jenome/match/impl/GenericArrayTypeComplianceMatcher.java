package org.harmony.jenome.match.impl;

import org.harmony.jenome.match.TypeComplianceMatcher;
import org.harmony.jenome.resolve.TypeVisitor;
import org.harmony.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.*;

/**
 * @author Denis Zhdanov
 */
public class GenericArrayTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<GenericArrayType> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitGenericArrayType(GenericArrayType type) {
            setMatched(getDelegate().match(
                    getBaseType().getGenericComponentType(), type.getGenericComponentType(), isStrict()
            ));
        }
    };

    public GenericArrayTypeComplianceMatcher() {
    }

    public GenericArrayTypeComplianceMatcher(TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}