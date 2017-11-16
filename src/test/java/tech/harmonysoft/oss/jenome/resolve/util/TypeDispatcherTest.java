package tech.harmonysoft.oss.jenome.resolve.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TypeDispatcherTest {

    private TypeDispatcher dispatcher = new TypeDispatcher();

    @Mock private TypeVisitor visitor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        dispatcher = new TypeDispatcher();
        visitor = mock(TypeVisitor.class);
    }

    @Test
    public void pureType() {
        Type type = mock(Type.class);
        dispatcher.dispatch(type, visitor);
        verify(visitor).visitType(type);
    }

    @Test
    public void pureParameterizedType() {
        ParameterizedType type = mock(ParameterizedType.class);
        dispatcher.dispatch(type, visitor);
        verify(visitor).visitParameterizedType(type);
    }

    @Test
    public void pureWildcardType() {
        WildcardType type = mock(WildcardType.class);
        dispatcher.dispatch(type, visitor);
        verify(visitor).visitWildcardType(type);
    }

    @Test
    public void pureGenericArrayType() {
        GenericArrayType type = mock(GenericArrayType.class);
        dispatcher.dispatch(type, visitor);
        verify(visitor).visitGenericArrayType(type);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pureTypeVariable() {
        TypeVariable type = mock(TypeVariable.class);
        dispatcher.dispatch(type, visitor);
        verify(visitor).visitTypeVariable(type);
    }

    @Test
    public void pureClass() {
        dispatcher.dispatch(Class.class, visitor);
        verify(visitor).visitClass(Class.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multipleMatches() {
        class TestClass implements TypeVariable, GenericArrayType {
            @Override
            public Type getGenericComponentType() {
                return null;
            }
            @Override
            public Type[] getBounds() {
                return new Type[0];
            }
            @Override
            public GenericDeclaration getGenericDeclaration() {
                return null;
            }
            @Override
            public String getName() {
                return null;
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds() {
                return new AnnotatedType[0];
            }
            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }
        }

        dispatcher.dispatch(new TestClass(), visitor);
        verify(visitor).visitTypeVariable(any(TypeVariable.class));
        verify(visitor).visitGenericArrayType(any(GenericArrayType.class));
    }
}