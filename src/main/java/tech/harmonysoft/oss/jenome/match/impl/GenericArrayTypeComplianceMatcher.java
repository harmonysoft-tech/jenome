package tech.harmonysoft.oss.jenome.match.impl;

import tech.harmonysoft.oss.jenome.match.TypeComplianceMatcher;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

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

    public GenericArrayTypeComplianceMatcher(@NotNull TypeComplianceMatcher<Type> delegate) {
        super(delegate);
    }

    @NotNull
    @Override
    protected TypeVisitor getVisitor() {
        return visitor;
    }
}