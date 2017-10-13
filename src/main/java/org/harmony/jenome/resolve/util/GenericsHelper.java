package org.harmony.jenome.resolve.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Stack;

/**
 * Holds various generics processing-related utility methods.
 * <p/>
 * This class is not singleton but offers single-point-of-usage field {@link #INSTANCE}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class GenericsHelper {

    public static final GenericsHelper INSTANCE = new GenericsHelper();

    /**
     * Assumes that class of given <code>'target'</code> object implements given generic
     * <code>'target interface'</code> and retrieves actual type of generic parameter of that target interface
     * for the given index.
     * <p/>
     * There is a possible case that 'raw' implementation class is used instead of generic one
     * (e.g. consider the following hierarchy:
     * <pre>
     * class Parent<T> implements Comparable<T> { // ... implementation}
     * class Child extends Parent {}
     * </pre>
     * Here <code>'Child'</code> class uses <code>'raw'</code> version of <code>'Parent'</code> class).
     * <code>'Object.class'</code> is returned then.
     *
     * @param targetInterface   target generic interface which type parameter we're interested in
     * @param target            target object that is assumed to implement given interface
     * @param index             target type parameter index (first index starts with zero)
     * @return                  type parameter of the given generic interface for the given index
     * @throws IllegalArgumentException     if any given reference is null or given index is negative or given
     *                                      interface is not generic or class of the given object doesn't implement
     *                                      given interface or given interface doesn't have enough type parameters
     *                                      as implied by the given index
     */
    public Type resolveTypeParameterValue(Class<?> targetInterface, Object target, int index)
            throws IllegalArgumentException
    {

        if (targetInterface == null) {
            throw new IllegalArgumentException("Can't derive target type parameter. Reason: given interface is null");
        }

        if (target == null) {
            throw new IllegalArgumentException(String.format("Can't derive type parameter #%d of the '%s' "
                                                             + "interface for the target object. Reason: given object is null", index, targetInterface));
        }

        if (!targetInterface.isAssignableFrom(target.getClass())) {
            throw new IllegalArgumentException(String.format("Can't derive type parameter #%d of the '%s' "
                                                             + "interface for the object of class '%s'. Reason: there is no IS-A relation between them",
                                                             index, targetInterface, target.getClass()));
        }

        if (index < 0) {
            throw new IllegalArgumentException(String.format("Can't derive target type parameter of the '%s' "
                                                             + "interface for the object of class '%s'. Reason: given index is negative (%d)",
                                                             targetInterface, target.getClass(), index));
        }

        if (targetInterface.getTypeParameters().length <= index) {
            throw new IllegalArgumentException(String.format("Can't derive type parameter #%d of the '%s' "
                                                             + "interface for the object of class '%s'. Reason: given interface doesn't have enough "
                                                             + "type parameters (%d type parameters are found)",
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
     * Allows to build stack of classes started from the given and ending by its first direct or indirect parent
     * that explicitly implements given interface.
     * <p/>
     * Note that target parent class is located at the top of resulting classes stack.
     * <p/>
     * Also note that it's assumed that given class IS-A given interface.
     *
     * @param targetInterface   target interface
     * @param startClass        class that is assumed to implements given interface
     * @return                  stack of the given class and its ancestors finishing by the class that directly
     *                          implements given interface
     */
    private Stack<Class<?>> getHierarchyFromDirectImplementation(Class<?> targetInterface, Class<?> startClass) {
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
     * This method assumes that given type variable belongs to the parameterized type that is located
     * on top of the given classes stack and that the stack holds particular classes hierarchy (parent is assumed
     * to be located at the top of the stack).
     * <p/>
     * It tries to resolve given type variable to any type that is not a type variable (e.g. concrete type class).
     * <p/>
     * If the method detects that one of the classes from the given hierarchy is not parameterized, it returns
     * <code>'Object.class'</code>.
     *
     * @param classes           target classes hierarchy
     * @param targetVariable    type variable to resolve
     * @return                  resolved type variable if any; <code>'Object.class'</code> otherwise
     */
    private Type resolveTypeVariable(Stack<Class<?>> classes, TypeVariable targetVariable) {
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