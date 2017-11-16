package tech.harmonysoft.oss.jenome.resolve.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Stack;

/**
 * <p>Holds various generics processing-related utility methods.</p>
 * <p>This class is not singleton but offers single-point-of-usage field {@link #INSTANCE}.</p>
 * <p>Thread-safe.</p>
 */
public class GenericsHelper {

    public static final GenericsHelper INSTANCE = new GenericsHelper();

    /**
     * <p>
     *      Assumes that class of given {@code 'target'} object implements given generic
     *      {@code 'target interface'} and retrieves actual type of generic parameter of that target interface
     *      for the given index.
     * </p>
     * <p>
     *      There is a possible case that 'raw' implementation class is used instead of generic one
     *      (e.g. consider the following hierarchy:
     * </p>
     * <pre>
     * class Parent<T> implements Comparable<T> { // ... implementation}
     * class Child extends Parent {}
     * </pre>
     * <p>
     *      Here {@code 'Child'} class uses {@code 'raw'} version of {@code 'Parent'} class).
     *      {@code 'Object.class'} is returned then.
     * </p>
     *
     * @param targetInterface   target generic interface which type parameter we're interested in
     * @param target            target object that is assumed to implement given interface
     * @param index             target type parameter index (first index starts with zero)
     * @return                  type parameter of the given generic interface for the given index
     * @throws IllegalArgumentException     if given index is negative or given interface is not generic or
     *                                      given object's class doesn't implement given interface or given
     *                                      interface doesn't have enough type parameters as implied
     *                                      by the given index
     */
    @NotNull
    public Type resolveTypeParameterValue(@NotNull Class<?> targetInterface, @NotNull Object target, int index)
            throws IllegalArgumentException
    {

        if (!targetInterface.isAssignableFrom(target.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "Can't derive type parameter #%d of the '%s' interface for the object of class '%s'. "
                    + "Reason: there is no IS-A relation between them", index, targetInterface, target.getClass()));
        }

        if (index < 0) {
            throw new IllegalArgumentException(String.format(
                    "Can't derive target type parameter of the '%s' interface for the object of class '%s'. "
                    + "Reason: given index is negative (%d)", targetInterface, target.getClass(), index));
        }

        if (targetInterface.getTypeParameters().length <= index) {
            throw new IllegalArgumentException(String.format(
                    "Can't derive type parameter #%d of the '%s' interface for the object of class '%s'. "
                    + "Reason: given interface doesn't have enough type parameters (%d type parameters are found)",
                    index, targetInterface, target.getClass(), targetInterface.getTypeParameters().length));
        }

        // The algorithm is the follows:
        //    1. Find top-level class in given object's class hierarchy that implements target interface;
        //    2. Try to find the first non-type variable target generic type parameter going down from the found class;

        Stack<Class<?>> classes = getHierarchyFromDirectImplementation(targetInterface, target.getClass());
        Type targetType = null;
        for (Type type : classes.pop().getGenericInterfaces()) {
            if (!(type instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() != targetInterface) {
                continue;
            }

            targetType = parameterizedType.getActualTypeArguments()[index];
            if (!(targetType instanceof TypeVariable)) {
                return targetType;
            }
        }

        // If we are here we know that there is a parent generic class that implements target interface and
        // target type parameter is a type variable, so, we try to check is there are child classes that define
        // that type variable as a concrete type.
        return targetType == null ? Object.class : resolveTypeVariable(classes, (TypeVariable) targetType);
    }

    /**
     * <p>
     *      Allows to build stack of classes started from the given and ending by its first direct or indirect parent
     *      that explicitly implements given interface.
     * </p>
     * <p>Note that target parent class is located at the top of resulting classes stack.</p>
     * <p>Also note that it's assumed that given class IS-A given interface.</p>
     *
     * @param targetInterface   target interface
     * @param startClass        class that is assumed to implements given interface
     * @return                  stack of the given class and its ancestors finishing by the class that directly
     *                          implements given interface
     */
    @NotNull
    private Stack<Class<?>> getHierarchyFromDirectImplementation(@NotNull Class<?> targetInterface,
                                                                 @NotNull Class<?> startClass)
    {
        assert targetInterface.isAssignableFrom(startClass);
        Stack<Class<?>> result = new Stack<Class<?>>();
        Class<?> classToCheck = startClass;
        while (classToCheck != null) {
            result.push(classToCheck);
            for (Class<?> i : classToCheck.getInterfaces()) {
                if (i == targetInterface) {
                    return result;
                }
            }
            classToCheck = classToCheck.getSuperclass();
        }
        return result;
    }

    /**
     * <p>
     *      This method assumes that given type variable belongs to the parameterized type that is located
     *      on top of the given classes stack and that the stack holds particular classes hierarchy (parent
     *      is assumed to be located at the top of the stack).
     * </p>
     * <p>
     *     It tries to resolve given type variable to any type that is not a type variable (e.g. concrete type class).
     * </p>
     * <p>
     *     If the method detects that one of the classes from the given hierarchy is not parameterized, it returns
     *      {@code 'Object.class'}.
     * </p>
     *
     * @param classes           target classes hierarchy
     * @param targetVariable    type variable to resolve
     * @return                  resolved type variable if any; {@code 'Object.class'} otherwise
     */
    @NotNull
    private Type resolveTypeVariable(@NotNull Stack<Class<?>> classes, @NotNull TypeVariable targetVariable) {
        Type result = null;
        while (!classes.isEmpty()) {
            int typeVariableIndex = -1;
            boolean found = false;
            for (TypeVariable typeVariable : targetVariable.getGenericDeclaration().getTypeParameters()) {
                ++typeVariableIndex;
                if (typeVariable == targetVariable) {
                    found = true;
                    break;
                }
            }
            assert found;
            Class<?> childClass = classes.pop();
            Type superClass = childClass.getGenericSuperclass();
            if (!(superClass instanceof ParameterizedType)) {
                return Object.class;
            }
            ParameterizedType parameterizedSuperClass = (ParameterizedType) superClass;
            result = parameterizedSuperClass.getActualTypeArguments()[typeVariableIndex];
            if (result instanceof TypeVariable) {
                targetVariable = (TypeVariable) result;
                continue;
            }
            return result;
        }
        return result == null ? targetVariable : result;
    }
}