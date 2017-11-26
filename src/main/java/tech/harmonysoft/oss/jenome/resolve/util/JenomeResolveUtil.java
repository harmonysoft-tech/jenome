package tech.harmonysoft.oss.jenome.resolve.util;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.impl.DefaultTypeArgumentResolver;
import tech.harmonysoft.oss.jenome.resolve.impl.TypeVisitorAdapter;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JenomeResolveUtil {

    private JenomeResolveUtil() {
    }

    /**
     * <p>
     *      Assumes that given object is an instance of a class which extends a single parameterized type/implements
     *      a single parameterized interface where that parameterized supertype has a single type argument.
     * </p>
     * <p>
     *     Current method allows to extract that type argument's value.
     * </p>
     * Example:
     * <pre>
     *     interface MyInterface&lt;T&gt; {}
     *     class MyClass implements MyInterface&lt;Integer&gt; {}
     *
     *     getTypeArgument(new MyClass()) -&gt; Integer.class
     * </pre>
     * <p>
     *     This method is assumed to be used during autowired elements organization, e.g.:
     * </p>
     * <pre>
     *     private final Map&lt;Type, MyInterface&lt;?&gt;&gt; handlersByPayloadType;
     *
     *    {@literal @}Autowired
     *     public MyService(Collection&lt;MyInterface&lt;?&gt;&gt; handlers) {
     *         handlersByPayloadType = handlers.stream()
     *                                         .collect(toMap(JenomeResolveUtil::getTypeArgument,
     *                                                        Function.identity()));
     *     }
     * </pre>
     *
     * @param o target object in which payload type we're interested in
     * @return  given object's payload type
     * @throws IllegalArgumentException     in case it's not possible to find out given object's payload type
     */
    @NotNull
    public static Type getTypeArgument(@NotNull Object o) throws IllegalArgumentException {
        AtomicReference<ParameterizedType> parameterizedTypeRef = new AtomicReference<>();
        Stack<Type> toProcess = new Stack<>();
        toProcess.push(o.getClass());
        TypeVisitor visitor = new TypeVisitorAdapter() {
            @Override
            public void visitParameterizedType(@NotNull ParameterizedType type) {
                ParameterizedType found = parameterizedTypeRef.get();
                if (found != null) {
                    throw new IllegalArgumentException(String.format(
                            "Expected to get an instance of a class which extends a single parameterized " +
                                    "type/implements a single parameterized interface but there are at least " +
                                    "two such parents for the given object '%s' of class %s: %s and %s",
                            o, o.getClass().getName(), found, type));
                }
                parameterizedTypeRef.set(type);
                toProcess.push(type.getRawType());
            }

            @Override
            public void visitClass(@NotNull Class<?> clazz) {
                for (Type type : clazz.getGenericInterfaces()) {
                    toProcess.push(type);
                }
            }
        };
        while (!toProcess.isEmpty()) {
            Type type = toProcess.pop();
            TypeDispatcher.INSTANCE.dispatch(type, visitor);
        }

        ParameterizedType parameterizedType = parameterizedTypeRef.get();
        if (parameterizedType == null) {
            throw new IllegalArgumentException(String.format(
                    "Expected to get an instance of a class which extends a single parameterized type/implements " +
                            "a single parameterized interface but there are no such parents for the given object " +
                            "'%s' of class %s ", o, o.getClass().getName()));
        }

        int typeArgumentsNumber = parameterizedType.getActualTypeArguments().length;
        if (typeArgumentsNumber != 1) {
            throw new IllegalArgumentException(String.format(
                    "Expected to get an instance of a class which extends a single parameterized type/implements " +
                            "a parameterized interface with a single type argument but target parent class (%s) " +
                            "has %d type arguments. Given object is '%s' of class %s ",
                    parameterizedType.getRawType(), typeArgumentsNumber, o, o.getClass().getName()));
        }

        Type result = DefaultTypeArgumentResolver.INSTANCE.resolve(parameterizedType, o.getClass(), 0);
        TypeDispatcher.INSTANCE.dispatch(result, new TypeVisitorAdapter() {
            @Override
            public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type) {
                throw new IllegalArgumentException(String.format(
                        "Expected to get an instance of a class which extends a single parameterized type/implements " +
                                "a parameterized interface with a single type argument. Given object '%s' of " +
                                "class %s extends a parameterized type %s but doesn't specify a concrete " +
                                "type argument value",
                        o, o.getClass().getName(), parameterizedType.getRawType()));
            }
        });
        return result;
    }

    /**
     * Calls {@link #getTypeArgument(Object)} for any given bean and returns a map where target type argument's value
     * is a key and given bean is a value
     *
     * @param beans     beans to process
     * @param <T>       target bean's type
     * @return          given beans organized by their type argument's values
     */
    @NotNull
    public static <T> Map<Type, T> byTypeValue(@NotNull Collection<T> beans) {
        return beans.stream().collect(Collectors.toMap(JenomeResolveUtil::getTypeArgument, Function.identity()));
    }
}
