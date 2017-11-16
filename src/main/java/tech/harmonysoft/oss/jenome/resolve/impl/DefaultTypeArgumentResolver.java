package tech.harmonysoft.oss.jenome.resolve.impl;

import tech.harmonysoft.oss.jenome.resolve.TypeArgumentResolver;
import tech.harmonysoft.oss.jenome.resolve.TypeVisitor;
import tech.harmonysoft.oss.jenome.resolve.util.TypeDispatcher;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Default {@link TypeArgumentResolver} implementation.</p>
 * <p>This class is not singleton but offers single-point-of-usage field ({@link #INSTANCE}).</p>
 * <p>Thread-safe.</p>
 */
public class DefaultTypeArgumentResolver implements TypeArgumentResolver {

    /** Single-point-of-usage field. */
    public static final DefaultTypeArgumentResolver INSTANCE = new DefaultTypeArgumentResolver();

    /** Builds actual type arguments mappings. */
    private final TypeVisitor typeArgumentsMappingBuilder = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            rememberMappings(type);
            if (type.getRawType() == baseClass.get()) {
                matched.set(true);
                return;
            }
            typeDispatcher.get().dispatch(type.getRawType(), this);
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            if (clazz == baseClass.get()) {
                matched.set(true);
                return;
            }

            interfaceFlag.set(true);
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            Class<?>[] rawInterfaces = clazz.getInterfaces();
            for (int i = 0; i < genericInterfaces.length; ++i) {
                if (!clazz.isInterface()) {
                    Map<Type, Type> map = interfaceArgumentsMap.get();
                    map.clear();
                    map.putAll(classArgumentsMap.get());
                }
                if (genericInterfaces[i] == rawInterfaces[i]) {
                    rememberRawMappings(rawInterfaces[i]);
                }
                typeDispatcher.get().dispatch(genericInterfaces[i], this);
                if (matched.get()) {
                    return;
                }
            }

            if (clazz.isInterface()) {
                // There is no point in asking interface for superclass
                return;
            }

            interfaceFlag.set(false);
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass == Object.class) {
                return;
            }
            Class<?> rawSuperclass = clazz.getSuperclass();
            if (genericSuperclass == rawSuperclass) {
                rememberRawMappings(rawSuperclass);
            }
            if (genericSuperclass != null) {
                typeDispatcher.get().dispatch(genericSuperclass, this);
            }
        }
    };

    /** Identifies base class to check. */
    private final TypeVisitor baseClassInitializer = new TypeVisitor() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            // We remember the mappings assuming that given type is not interface but class. They are moved to the
            // corresponding collection from visitClass() otherwise.
            rememberMappings(type);
            typeDispatcher.get().dispatch(type.getRawType(), this);
        }

        @Override
        public void visitWildcardType(@NotNull WildcardType type) throws IllegalArgumentException {
            throw new IllegalArgumentException(getErrorMessage(WildcardType.class));
        }

        @Override
        public void visitGenericArrayType(@NotNull GenericArrayType type) throws IllegalArgumentException {
            throw new IllegalArgumentException(getErrorMessage(GenericArrayType.class));
        }

        @Override
        public void visitTypeVariable(@NotNull TypeVariable<? extends GenericDeclaration> type)
                throws IllegalArgumentException
        {
            throw new IllegalArgumentException(getErrorMessage(TypeVariable.class));
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            // We know that parameterized type mappings are remembered at 'class' map by default, so,
            // if target raw type is interface just move the mappings to 'interface' map.
            if (clazz.isInterface()) {
                interfaceArgumentsMap.get().putAll(classArgumentsMap.get());
                classArgumentsMap.get().clear();
            }
            baseClass.set(clazz);
        }

        @Override
        public void visitType(@NotNull Type type) throws IllegalArgumentException {
            throw new IllegalArgumentException(getErrorMessage(Type.class));
        }

        private String getErrorMessage(Class<?> targetClass) {
            return String.format("Type argument resolving rule from '%s' type undefined", targetClass);
        }
    };

    /** This visitor is served for correct {@link #interfaceFlag} initialization for the 'target' type */
    private final TypeVisitor interfaceFlagInitializer = new TypeVisitorAdapter() {
        @Override
        public void visitParameterizedType(@NotNull ParameterizedType type) {
            typeDispatcher.get().dispatch(type.getRawType(), this);
        }

        @Override
        public void visitClass(@NotNull Class<?> clazz) {
            interfaceFlag.set(clazz.isInterface());
        }
    };

    private final ThreadLocal<Map<Type, Type>> classArgumentsMap = new ThreadLocal<Map<Type, Type>>() {
        @Override
        protected Map<Type, Type> initialValue() {
            return new LinkedHashMap<Type, Type>();
        }
    };

    private final ThreadLocal<Map<Type, Type>> interfaceArgumentsMap = new ThreadLocal<Map<Type, Type>>() {
        @Override
        protected Map<Type, Type> initialValue() {
            return new LinkedHashMap<Type, Type>();
        }
    };

    private final ThreadLocal<Map<Type, Type>> tempMap = new ThreadLocal<Map<Type, Type>>() {
        @Override
        protected Map<Type, Type> initialValue() {
            return new LinkedHashMap<Type, Type>();
        }
    };

    private final ThreadLocal<Boolean> interfaceFlag = new ThreadLocal<Boolean>();
    private final ThreadLocal<Boolean> matched = new ThreadLocal<Boolean>();
    private final ThreadLocal<Class<?>> baseClass = new ThreadLocal<Class<?>>();
    private final AtomicReference<TypeDispatcher> typeDispatcher
                                                     = new AtomicReference<TypeDispatcher>(TypeDispatcher.INSTANCE);

    @NotNull
    @Override
    public Type resolve(@NotNull Type base, @NotNull Type target, int index) throws IllegalArgumentException {
        if (index < 0) {
            throw new IllegalArgumentException(String.format(
                    "Can't resolve type parameter of the type '%s' against type '%s'. Reason: given index "
                    + "is negative (%d)", base, target, index));
        }

        interfaceFlag.set(false);
        typeDispatcher.get().dispatch(base, baseClassInitializer);
        if (baseClass.get().getTypeParameters().length <= index) {
            throw new IllegalArgumentException(String.format(
                    "Can't resolve type parameter of the type '%s' against type '%s'. Reason: given index "
                    + "is too big (%d). Available type arguments number is %d",
                    base, target, index, baseClass.get().getTypeParameters().length));
        }

        typeDispatcher.get().dispatch(target, interfaceFlagInitializer);
        matched.set(false);
        typeDispatcher.get().dispatch(target, typeArgumentsMappingBuilder);

        if (!matched.get()) {
            throw new IllegalArgumentException(String.format(
                    "Can't resolve type parameter #%d of the type '%s' against type '%s'. Reason: there "
                    + "is no IS-A relation between them", index, base, target));
        }

        Map<Type, Type> argumentTypes = baseClass.get().isInterface()
                                        ? interfaceArgumentsMap.get() : classArgumentsMap.get();
        for (Map.Entry<Type, Type> entry : argumentTypes.entrySet()) {
            if (--index < 0) {
                return entry.getValue();
            }
        }
        return RAW_TYPE;
    }

    /**
     * <p>Allows to define custom type dispatcher to use.</p>
     * <p>{@link TypeDispatcher#INSTANCE} is used by default.</p>
     *
     * @param typeDispatcher    custom type dispatcher to use
     */
    public void setTypeDispatcher(@NotNull TypeDispatcher typeDispatcher) {
        this.typeDispatcher.set(typeDispatcher);
    }

    private void rememberMappings(@NotNull ParameterizedType type) {
        Type[] actualArguments = type.getActualTypeArguments();
        Class<?> clazz = (Class<?>) type.getRawType();
        TypeVariable<? extends Class<?>>[] typeVariables = clazz.getTypeParameters();
        rememberMappings(typeVariables, actualArguments);
    }

    private void rememberRawMappings(@NotNull Class<?> rawClass) {
        Type[] typeVariables = rawClass.getTypeParameters();
        Type[] rawVariables = new Type[typeVariables.length];
        Arrays.fill(rawVariables, RAW_TYPE);
        rememberMappings(typeVariables, rawVariables);
    }

    private void rememberMappings(@NotNull Type[] from, @NotNull Type[] to) {
        Map<Type, Type> previousMappings = interfaceFlag.get() ? interfaceArgumentsMap.get() : classArgumentsMap.get();
        Map<Type, Type> currentMappings = tempMap.get();
        for (int i = 0; i < from.length; ++i) {
            Type resolved = to[i];
            if (previousMappings.containsKey(resolved)) {
                resolved = previousMappings.get(resolved);
            }
            currentMappings.put(from[i], resolved);
        }
        previousMappings.clear();
        previousMappings.putAll(currentMappings);
        currentMappings.clear();
    }
}