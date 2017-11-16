package tech.harmonysoft.oss.jenome.resolve.util;

import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.*;

/**
 * @author Denis Zhdanov
 */
public class TypeDispatcherTest {

    private TypeDispatcher dispatcher;
    private Mockery        mockery;
    private TypeVisitor    visitor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        dispatcher = new TypeDispatcher();
        mockery = new Mockery();
        visitor = mockery.mock(TypeVisitor.class);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullType() {
        dispatcher.dispatch(null, visitor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullVisitor() {
        dispatcher.dispatch(String.class, null);
    }

    @Test
    public void pureType() {
        mockery.checking(new Expectations() {{
            one(visitor).visitType(with(any(Type.class)));
        }});
        dispatcher.dispatch(mockery.mock(Type.class), visitor);
    }

    @Test
    public void pureParameterizedType() {
        mockery.checking(new Expectations() {{
            one(visitor).visitParameterizedType(with(any(ParameterizedType.class)));
        }});
        dispatcher.dispatch(mockery.mock(ParameterizedType.class), visitor);
    }

    @Test
    public void pureWildcardType() {
        mockery.checking(new Expectations() {{
            one(visitor).visitWildcardType(with(any(WildcardType.class)));
        }});
        dispatcher.dispatch(mockery.mock(WildcardType.class), visitor);
    }

    @Test
    public void pureGenericArrayType() {
        mockery.checking(new Expectations() {{
            one(visitor).visitGenericArrayType(with(any(GenericArrayType.class)));
        }});
        dispatcher.dispatch(mockery.mock(GenericArrayType.class), visitor);
    }

    @Test
    public void pureTypeVariable() {
        mockery.checking(new Expectations() {{
            one(visitor).visitTypeVariable(with(any(TypeVariable.class)));
        }});
        dispatcher.dispatch(mockery.mock(TypeVariable.class), visitor);
    }

    @Test
    public void pureClass() {
        mockery.checking(new Expectations() {{
            one(visitor).visitClass(with(any(Class.class)));
        }});
        dispatcher.dispatch(Class.class, visitor);
    }

    @Test
    public void multipleMatches() {
//        class TestClass implements TypeVariable, GenericArrayType {
//            @Override
//            public Type getGenericComponentType() {
//                return null;
//            }
//            @Override
//            public Type[] getBounds() {
//                return new Type[0];
//            }
//            @Override
//            public GenericDeclaration getGenericDeclaration() {
//                return null;
//            }
//            @Override
//            public String getName() {
//                return null;
//            }
//
//            @Override
//            public AnnotatedType[] getAnnotatedBounds() {
//                return new AnnotatedType[0];
//            }
//            @Override
//            public Annotation[] getDeclaredAnnotations() {
//                return new Annotation[0];
//            }
//        }
//
//        mockery.checking(new Expectations() {{
//            one(visitor).visitTypeVariable(with(any(TypeVariable.class)));
//            one(visitor).visitGenericArrayType(with(any(GenericArrayType.class)));
//        }});
//        dispatcher.dispatch(new TestClass(), visitor);
    }
}