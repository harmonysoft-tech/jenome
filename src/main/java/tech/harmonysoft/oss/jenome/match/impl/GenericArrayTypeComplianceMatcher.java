package tech.harmonysoft.oss.jenome.match.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

public class GenericArrayTypeComplianceMatcher extends AbstractDelegatingTypeComplianceMatcher<GenericArrayType> {

    private final TypeVisitor visitor = new TypeVisitorAdapter() {
        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) {
            setMatched(getDelegate().match(
                    getBaseType().getGenericComponentType(), type.getGenericComponentType(), isStrict()
            ));
        }
    };

    public GenericArrayTypeComplianceMatcher() {
    }

    public GenericArrayTypeComplianceMatcher(@NotNull AbstractTypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}