package fi.helsinki.cs.tmc.edutestutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Provides the features of {@link ReflectionUtils} as a cute typesafe/IDE-friendly DSL.
 * 
 * <p>
 * Usage in a nutshell:
 * <pre>
 * {@code
 * // Get a ClassRef, which is the entry point to the DSL.
 * ClassRef<Thing> thingCls = Reflex.reflect(Thing.class);
 * // Use the DSL to get a specific constructor and call it.
 * Thing thing = thingCls.constructor().taking(int.class).invoke(123);
 * 
 * // Now find some specific methods and call those.
 * thingCls.method(thing, "setFoo").returningVoid().taking(int.class).invoke(5);
 * int result = thingCls.method(thing, "getFoo").returning(int.class).takingNoParams().invoke();
 * }
 * </pre>
 * 
 * <p>
 * References to methods and constructors can be stored in variables and called on multiple objects.
 * <pre>
 * {@code
 * MethodRef1<Thing, Void, Integer> setFoo =
 *     Reflex.reflect(Thing.class)
 *         .method("setFoo") // no this parameter as above
 *         .returningVoid()
 *         .taking(int.class);
 * setFoo.invokeOn(instance1, 10);
 * setFoo.invokeOn(instance2, 20);
 * }
 * </pre>
 * 
 * <p>
 * In general, <tt>MethodRef<i>N</i></tt> refers to a constructor or
 * a static or non-static method taking N parameters.
 * The first type parameter is the receiver object's type, the second the
 * return type and the rest the types of the parameters.
 * 
 * <p>
 * Reflex also works on classes loaded at runtime.
 * There are two ways to assign type parameters in this case.
 * One way is to just use {@code Object} in place of the type in type parameters.
 * <pre>
 * {@code
 * ClassRef<Object> thingCls = Reflex.reflect("Thing"); // Load class `Thing` at runtime
 * Object thing = thingCls.constructor().taking(int.class).invoke(123);
 * 
 * MethodRef1<Object, Void, Integer> setFoo =  // Use Object as the first type parameter in MethodRefs
 *     thingCls
 *         .method(thing, "setFoo")
 *         .returningVoid()
 *         .taking(int.class);
 * setFoo.invoke(123);
 * }
 * </pre>
 * 
 * <p>
 * Perhaps a nicer alternative is to add a type parameter to your test class and use that to represent the loaded class.
 * <pre>
 * {@code
 * public class MyTest<_Thing> {
 *     ...
 *     ClassRef<_Thing> thingCls = Reflex.reflect("Thing");
 *     ...
 * }
 * }
 * </pre>
 * 
 * <p>
 * Just remeber that in the above, {@code _Thing} is not the same class as {@code Thing}
 * since they're loaded by different class loaders.
 * 
 * <p>
 * Note that obtaining a MethodRef will always succeed even if the method doesn't exist.
 * The method's existence is of course checked when it's invoked, but you can also
 * check it explicitly.
 * <pre>
 * {@code
 * MethodRef0<Thing, Void> m = Reflex.reflect(Thing.class).method("foo").returningVoid().takingNoParams();
 * if (!m.exists()) {
 *     fail("Method foo not found");
 * }
 * // Or alternatively
 * m.requireExists();
 * // Or, to require it exists and is public
 * m.requirePublic();
 * }
 * </pre>
 * 
 * <p>
 * Reflex will allow you to invoke non-public methods. It will not check that a method is public by default.
 * 
 * <p>
 * Reflex uses {@link ReflectionUtils} internally and gains its localized error
 * messages for the exceptions it throws.
 * 
 * @see ReflectionUtils
 */
public class Reflex {
    private Reflex() {
    }
    
    /**
     * Enters the DSL.
     * 
     * <p>
     * See examples in the class docs. 
     */
    public static <S> ClassRef<S> reflect(Class<S> cls) {
        if (cls == null) {
            throw new NullPointerException("Class cannot be null");
        }
        return new ClassRef<S>(cls);
    }
    
    /**
     * Enters the DSL with a class loaded at runtime.
     * 
     * <p>
     * See examples in the class docs. 
     */
    @SuppressWarnings("unchecked")
    public static <T> ClassRef<T> reflect(String className) {
        return new ClassRef<T>((Class<T>)ReflectionUtils.findClass(className));
    }
    
    
    static enum MethodType { CONSTRUCTOR, METHOD, STATIC_METHOD };
    
    /**
     * Refers to a reflected class.
     */
    public static class ClassRef<S> {
        private final Class<S> cls;
        
        ClassRef(Class<S> cls) {
            this.cls = cls;
        }
        
        /**
         * Returns the referenced class object.
         */
        public Class<S> getReferencedClass() {
            return cls;
        }
        
        
        /**
         * Returns the referenced class object.
         * 
         * <p>
         * An alias for {@link #getReferencedClass()}.
         */
        public Class<S> cls() {
            return cls;
        }
        
        /**
         * Tells whether the referenced class extends or implements the given class or interface.
         */
        public boolean inherits(Class<?> superClassOrInterface) {
            return superClassOrInterface.isAssignableFrom(this.cls);
        }
        
        /**
         * Tells whether the referenced class extends or implements the given class or interface.
         */
        public boolean inherits(ClassRef<?> superClassOrInterfaceRef) {
            return superClassOrInterfaceRef.getReferencedClass().isAssignableFrom(this.cls);
        }
        
        /**
         * Selects constructors and continues with {@code .taking(...)}.
         * 
         * <p>
         * Shorthand for {@link #constructor()}.
         */
        public MethodAndReturnType<S, S> ctor() {
            return new MethodAndReturnType<S, S>(cls, null, MethodType.CONSTRUCTOR, null, cls);
        }
        
        /**
         * Selects constructors and continues with {@code .taking(...)}.
         */
        public MethodAndReturnType<S, S> constructor() {
            return new MethodAndReturnType<S, S>(cls, null, MethodType.CONSTRUCTOR, null, cls);
        }
        
        /**
         * Selects non-static methods, with {@code this} to be given later, and continues with {@code .returning(...)}.
         * 
         * <p>
         * If you use this form to obtain a method reference then call it with
         * {@code invokeOn} to specify a this parameter.
         */
        public MethodName<S> method(String name) {
            return method(null, name);
        }
        
        /**
         * Selects non-static methods and continues with {@code .returning(...)}.
         */
        public MethodName<S> method(S self, String name) {
            if (name == null) {
                throw new NullPointerException("Method name cannot be null");
            }
            return new MethodName<S>(cls, self, MethodType.METHOD, name);
        }
        
        /**
         * Selects static methods and continues with {@code .returning(...)}.
         */
        public MethodName<S> staticMethod(String name) {
            return new MethodName<S>(cls, null, MethodType.STATIC_METHOD, name);
        }
    }
    
    /**
     * Refers to a method or constructor in a class.
     * 
     * @param <S> The class containing the method.
     */
    public static class MethodName<S> {
        private final Class<? extends S> cls;
        private final S self;
        private final MethodType methodType;
        private final String name;

        MethodName(Class<? extends S> cls, S self, MethodType methodType, String name) {
            this.cls = cls;
            this.self = self;
            this.name = name;
            this.methodType = methodType;
        }
        
        /**
         * Specifies a return type and continues with {@code taking(...)}.
         */
        public <R> MethodAndReturnType<S, R> returning(Class<? extends R> returnType) {
            return new MethodAndReturnType<S, R>(cls, self, methodType, name, returnType);
        }
        
        /**
         * Specifies a void return and continues with {@code taking(...)}.
         */
        public MethodAndReturnType<S, Void> returningVoid() {
            return new MethodAndReturnType<S, Void>(cls, self, methodType, name, Void.TYPE);
        }
    }
    
    /**
     * Refers to a method or constructor in a class and stores the expected result type.
     * 
     * @param <S> The class containing the method.
     * @param <R> The expected return type.
     */
    public static class MethodAndReturnType<S, R> {
        private final Class<? extends S> cls;
        private final S self;
        private final MethodType methodType;
        private final String name;
        private final Class<? extends R> returnType;

        MethodAndReturnType(Class<? extends S> cls, S self, MethodType methodType, String name, Class<? extends R> returnType) {
            this.cls = cls;
            this.self = self;
            this.methodType = methodType;
            this.name = name;
            this.returnType = returnType;
        }
        
        /**
         * Specifies no expected parameters.
         */
        public MethodRef0<S, R> takingNoParams() {
            return new MethodRef0<S, R>(this);
        }
        
        /**
         * Specifies 1 expected parameter.
         */
        public <P1> MethodRef1<S, R, P1> taking(Class<P1> p1) {
            return new MethodRef1<S, R, P1>(this, p1);
        }
        
        /**
         * Specifies 2 expected parameters.
         */
        public <P1, P2> MethodRef2<S, R, P1, P2> taking(Class<P1> p1, Class<P2> p2) {
            return new MethodRef2<S, R, P1, P2>(this, p1, p2);
        }
        
        /**
         * Specifies 3 expected parameters.
         */
        public <P1, P2, P3> MethodRef3<S, R, P1, P2, P3> taking(Class<P1> p1, Class<P2> p2, Class<P3> p3) {
            return new MethodRef3<S, R, P1, P2, P3>(this, p1, p2, p3);
        }
        
        /**
         * Specifies 4 expected parameters.
         */
        public <P1, P2, P3, P4> MethodRef4<S, R, P1, P2, P3, P4> taking(Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4) {
            return new MethodRef4<S, R, P1, P2, P3, P4>(this, p1, p2, p3, p4);
        }
        
        /**
         * Specifies 5 expected parameters.
         */
        public <P1, P2, P3, P4, P5> MethodRef5<S, R, P1, P2, P3, P4, P5> taking(Class<P1> p1, Class<P2> p2, Class<P3> p3, Class<P4> p4, Class<P5> p5) {
            return new MethodRef5<S, R, P1, P2, P3, P4, P5>(this, p1, p2, p3, p4, p5);
        }
    }
    
    /**
     * Refers to a method or constructor in a class and stores the expected result and parameter types.
     * 
     * @param <S> The class containing the method.
     * @param <R> The expected return type.
     */
    public static abstract class MethodRef<S, R> implements Cloneable {
        private final MethodAndReturnType<S, R> method;
        private final Class<?>[] paramTypes;

        MethodRef(MethodAndReturnType<S, R> method, Class<?> ... paramTypes) {
            for (Class<?> cls : paramTypes) {
                if (cls == null) {
                    throw new NullPointerException("Parameter type cannot be null");
                }
            }
            
            this.method = method;
            this.paramTypes = paramTypes;
        }
        
        /**
         * Tells whether this method or constructor exists.
         */
        public boolean exists() {
            try {
                requireExists();
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }
        
        /**
         * Tells whether this method exists and is public.
         */
        public boolean isPublic() {
            return hasModifier(Modifier.PUBLIC);
        }
        
        /**
         * Tells whether this method exists and is protected.
         */
        public boolean isProtected() {
            return hasModifier(Modifier.PROTECTED);
        }
        
        /**
         * Tells whether this method exists and is private.
         */
        public boolean isPrivate() {
            return hasModifier(Modifier.PRIVATE);
        }
        
        /**
         * Tells whether this method exists and is package private.
         */
        public boolean isPackagePrivate() {
            return !hasModifier(Modifier.PUBLIC) &&
                    !hasModifier(Modifier.PROTECTED) &&
                    !hasModifier(Modifier.PRIVATE);
        }
        
        /**
         * Throws {@link AssertionError} if this method or constructor doesn't exist.
         */
        public void requireExists() throws AssertionError {
            requireExists(null);
        }
        
        /**
         * Throws {@link AssertionError} if this method or constructor doesn't exist or is not public.
         */
        public void requirePublic() throws AssertionError {
            requireExists(ReflectionUtils.PUBLIC);
        }
        
        /**
         * Throws {@link AssertionError} if this method or constructor doesn't exist or is not protected.
         */
        public void requireProtected() throws AssertionError {
            requireExists(ReflectionUtils.PROTECTED);
        }
        
        /**
         * Throws {@link AssertionError} if this method or constructor doesn't exist or is not private.
         */
        public void requirePrivate() throws AssertionError {
            requireExists(ReflectionUtils.PRIVATE);
        }
        
        /**
         * Throws {@link AssertionError} if this method or constructor doesn't exist or is not package private.
         */
        public void requirePackagePrivate() throws AssertionError {
            requireExists(ReflectionUtils.PACKAGE_PRIVATE);
        }
        
        /**
         * Finds and returns the underlying {@code java.lang.reflect.Method} or throws an exception.
         */
        public Method getMethod()
        {
            switch (method.methodType) {
                case METHOD:
                    return ReflectionUtils.requireMethod(null, false, method.cls, method.returnType, method.name, paramTypes);
                case STATIC_METHOD:
                    return ReflectionUtils.requireMethod(null, true, method.cls, method.returnType, method.name, paramTypes);
                default:
                    throw new IllegalStateException("getMethod() called on a constructor");
            }
        }
        
        /**
         * Finds and returns the underlying {@code java.lang.reflect.Constructor} or throws an exception.
         */
        public Constructor<? extends S> getConstructor()
        {
            switch (method.methodType) {
                case CONSTRUCTOR:
                    return ReflectionUtils.requireConstructor(null, method.cls, paramTypes);
                default:
                    throw new IllegalStateException("getConstructor() called on a method");
            }
        }
        
        /**
         * Returns a human-readable signature of the method.
         */
        public String signature() {
            switch (method.methodType) {
                case CONSTRUCTOR: return ReflectionUtils.niceMethodSignature(method.cls.getSimpleName(), paramTypes);
                case METHOD: return ReflectionUtils.niceMethodSignature(method.returnType, method.name, paramTypes);
                case STATIC_METHOD: return "static " + ReflectionUtils.niceMethodSignature(method.returnType, method.name, paramTypes);
                default: throw new IllegalStateException("Implementation error in Reflex.");
            }
        }
        
        @SuppressWarnings("unchecked")
        protected R invokeImpl(Object... params) throws Throwable {
            switch (method.methodType) {
                case CONSTRUCTOR: return (R)invokeCtor(params);
                case METHOD: return (R)invokeMethod(params);
                case STATIC_METHOD: return (R)invokeMethod(params);
                default: throw new IllegalStateException("Implementation error in Reflex.");
            }
        }
        
        @SuppressWarnings("unchecked")
        protected R invokeOnImpl(Object self, Object... params) throws Throwable {
            switch (method.methodType) {
                case METHOD: return (R)invokeMethodOn(self, params);
                default: throw new IllegalStateException("This paremeter provided to something that isn't a non-static method.");
            }
        }
        
        private Object invokeCtor(Object... params) throws Throwable {
            Constructor<? extends S> ctor = getConstructor();
            return ReflectionUtils.invokeConstructor(ctor, params);
        }
        
        private Object invokeMethod(Object... params) throws Throwable {
            return invokeMethodOn(method.self, params);
        }
        
        private Object invokeMethodOn(Object self, Object... params) throws Throwable {
            if (method.methodType == MethodType.METHOD && self == null) {
                throw new NullPointerException("Trying to invoke a method without a this parameter. Provide a this parameter or use invokeOn.");
            }
            
            return ReflectionUtils.invokeMethod(method.returnType, getMethod(), self, params);
        }
        
        private void requireExists(Integer expectedAccess) throws AssertionError {
            switch (method.methodType) {
                case CONSTRUCTOR:
                    ReflectionUtils.requireConstructor(expectedAccess, method.cls, paramTypes);
                    break;
                case METHOD:
                    ReflectionUtils.requireMethod(expectedAccess, false, method.cls, method.returnType, method.name, paramTypes);
                    break;
                case STATIC_METHOD:
                    ReflectionUtils.requireMethod(expectedAccess, true, method.cls, method.returnType, method.name, paramTypes);
                break;
                default: throw new IllegalStateException("Implementation error in Reflex.");
            }
        }
        
        private boolean hasModifier(int mod) {
            Integer m = getModifiers();
            if (m != null) {
                return (m & mod) != 0;
            } else {
                return false;
            }
        }
        
        private Integer getModifiers() {
            if (exists()) {
                if (method.methodType == MethodType.CONSTRUCTOR) {
                    return getConstructor().getModifiers();
                } else {
                    return getMethod().getModifiers();
                }
            } else {
                return null;
            }
        }
    }
    
    public static class MethodRef0<S, R> extends MethodRef<S, R> {
        MethodRef0(MethodAndReturnType<S, R> m) {
            super(m);
        }
        
        /**
         * Invokes the method.
         * 
         * If the method is non-static and not a constructor then a
         * {@code this} parameter must have been given earlier.
         */
        public R invoke() throws Throwable {
            return invokeImpl();
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self) throws Throwable {
            return invokeOnImpl(self);
        }
    }
    
    public static class MethodRef1<S, R, P1> extends MethodRef<S, R> {
        MethodRef1(MethodAndReturnType<S, R> m, Class<P1> p1Type) {
            super(m, p1Type);
        }
        
        /**
         * Invokes the method.
         * 
         * If the method is non-static and not a constructor then a
         * {@code this} parameter must have been given earlier.
         */
        public R invoke(P1 p1) throws Throwable {
            return invokeImpl(p1);
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self, P1 p1) throws Throwable {
            return invokeOnImpl(self, p1);
        }
    }
    
    public static class MethodRef2<S, R, P1, P2> extends MethodRef<S, R> {
        MethodRef2(MethodAndReturnType<S, R> m, Class<P1> p1Type, Class<P2> p2Type) {
            super(m, p1Type, p2Type);
        }
        
        /**
         * Invokes the method.
         * 
         * If the method is non-static and not a constructor then a
         * {@code this} parameter must have been given earlier.
         */
        public R invoke(P1 p1, P2 p2) throws Throwable {
            return invokeImpl(p1, p2);
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self, P1 p1, P2 p2) throws Throwable {
            return invokeOnImpl(self, p1, p2);
        }
    }
    
    public static class MethodRef3<S, R, P1, P2, P3> extends MethodRef<S, R> {
        MethodRef3(MethodAndReturnType<S, R> m, Class<P1> p1Type, Class<P2> p2Type, Class<P3> p3Type) {
            super(m, p1Type, p2Type, p3Type);
        }
        
        /**
         * Invokes the method.
         * 
         * If the method is non-static and not a constructor then a
         * {@code this} parameter must have been given earlier.
         */
        public R invoke(P1 p1, P2 p2, P3 p3) throws Throwable {
            return invokeImpl(p1, p2, p3);
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self, P1 p1, P2 p2, P3 p3) throws Throwable {
            return invokeOnImpl(self, p1, p2, p3);
        }
    }
    
    public static class MethodRef4<S, R, P1, P2, P3, P4> extends MethodRef<S, R> {
        MethodRef4(MethodAndReturnType<S, R> m, Class<P1> p1Type, Class<P2> p2Type, Class<P3> p3Type, Class<P4> p4Type) {
            super(m, p1Type, p2Type, p3Type, p4Type);
        }
        
        public R invoke(P1 p1, P2 p2, P3 p3, P4 p4) throws Throwable {
            return invokeImpl(p1, p2, p3, p4);
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self, P1 p1, P2 p2, P3 p3, P4 p4) throws Throwable {
            return invokeOnImpl(self, p1, p2, p3, p4);
        }
    }
    
    public static class MethodRef5<S, R, P1, P2, P3, P4, P5> extends MethodRef<S, R> {
        MethodRef5(MethodAndReturnType<S, R> m, Class<P1> p1Type, Class<P2> p2Type, Class<P3> p3Type, Class<P4> p4Type, Class<P5> p5Type) {
            super(m, p1Type, p2Type, p3Type, p4Type, p5Type);
        }
        
        public R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Throwable {
            return invokeImpl(p1, p2, p3, p4, p5);
        }
        
        /**
         * Invokes the method with a given {@code this} parameter.
         */
        public R invokeOn(S self, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Throwable {
            return invokeOnImpl(self, p1, p2, p3, p4, p5);
        }
    }
}
