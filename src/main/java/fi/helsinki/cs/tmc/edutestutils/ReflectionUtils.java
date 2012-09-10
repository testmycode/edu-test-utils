package fi.helsinki.cs.tmc.edutestutils;

import fi.helsinki.cs.tmc.edutestutils.classloaders.SingleClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Helpers for testing a student's class entirely through reflection
 * with nice error messages for missing methods and other mistakes.
 * 
 * <p>
 * You probably want to use the nicer interface, {@link Reflex}, instead of
 * this most of the time.
 * 
 * <p>
 * All methods throw AssertionError with a friendly error message on failure
 * (or {@link IllegalArgumentException} if used incorrectly),
 * so you can use them as if they were assertions.
 * 
 * <p>
 * NOTE: a common mistake is to forget the difference between {@code Integer.class} and
 * {@code int.class}.
 * 
 * @see Reflex
 */
public class ReflectionUtils {

    private static Locale msgLocale;
    private static ResourceBundle msgBundle;
    
    public static final int PUBLIC = Modifier.PUBLIC;
    public static final int PROTECTED = Modifier.PROTECTED;
    public static final int PRIVATE = Modifier.PRIVATE;
    public static final int PACKAGE_PRIVATE = 0xF0000000;
    private static final int[] ALL_ACCESS_MODIFIERS = { PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE };
    private static final String[] ALL_ACCESS_MODIFIERS_STR = { "public", "protected", "private", "package_private" };
    
    static {
        EduTestUtilsDefaultLocale.addListener(new EduTestUtilsDefaultLocale.Listener() {
            public void eduTestUtilsLocaleChanged(Locale newLocale) {
                loadMsgBundle();
            }
        });
        
        loadMsgBundle();
    }
    
    private ReflectionUtils() {
    }
    
    private static void loadMsgBundle() {
        msgLocale = EduTestUtilsDefaultLocale.get();
        msgBundle = ResourceBundle.getBundle(ReflectionUtils.class.getCanonicalName(), msgLocale);
    }
    
    private static String tr(String key, Object... args) {
        String[] argStrs = new String[args.length];
        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Class<?>) {
                argStrs[i] = ((Class<?>)args[i]).getSimpleName();
            } else {
                argStrs[i] = args[i].toString();
            }
        }
        return new MessageFormat(msgBundle.getString(key), msgLocale).format(argStrs);
    }
    
    /**
     * Finds a class.
     * 
     * <p>
     * Uses the system class loader.
     * 
     * @param name The fully qualified name of the class.
     * @return The class object. Never null.
     * @throws AssertionError If the class could not be found.
     */
    public static Class<?> findClass(String name) {
        if (name.contains("/")) {
            throw new IllegalArgumentException("Test writer: use '.' as the package separator instead of '/'.");
        }
        
        return loadClassWith(name, ClassLoader.getSystemClassLoader());
    }
    
    /**
     * Throws an exception if the access modifiers of the given class are wrong.
     * 
     * @param cls A class whose access modifiers to check.
     * @param expectedAccess One or more (OR-ed) of PUBLIC, PROTECTED, PRIVATE or PACKAGE_PRIVATE to allow, or null to not check.
     */
    public static void requireClassAccess(Class<?> cls, Integer expectedAccess) {
        if (!isExpectedAccess(expectedAccess, cls.getModifiers())) {
            throw new AssertionError(tr("class_wrong_access", cls.getName(), accessModifiersToString(expectedAccess)));
        }
    }
    
    /**
     * Loads a new instance of the class in a new class loader.
     * 
     * <p>
     * First of all, consider this method error-prone and avoid it if possible.
     * 
     * <p>
     * The intended use case is to allow rerunning static initializers
     * in student code. The new class instance returned by this method is
     * <b>uninitialized</b>, i.e. its static initializers have not been run
     * (JVM spec 2nd ed. <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19075">§2.17.4</a>).
     * To run static initializers, call a method on the new class instance or
     * read the value of a non-final static field.
     * 
     * <p>
     * The following shows how to load a new instance of a class
     * <tt>Main</tt> and reinitialize it as a side effect of calling
     * its <tt>main</tt> method.
     * 
     * <pre>
     * {@code
     * Class<?> reloadedMain = ReflectionUtils.newInstanceOfClass(Main.class.getName());
     * Method mainMethod = requireMethod(reloadedMain, "main", String[].class);
     * invokeMethod(void.class, mainMethod, new String[0]); // statics are reinitialized here
     * }
     * </pre>
     * 
     * <p>
     * The new instance of the class is loaded by a fresh class loader.
     * This means that it is <b>incompatible</b> with any previously loaded
     * instance of the class and should only be accessed reflectively.
     * That is, the following example will throw a {@link ClassCastException}!
     * 
     * <pre>
     * {@code
     * Object reloadedThing = ReflectionUtils.newInstanceOfClass("Thing").newInstance();
     * Thing thing = (Thing)reloadedThing;
     * }
     * </pre>
     * 
     * <p>
     * Indeed a class in Java is uniquely identified by its name <b>and</b>
     * the classloader that loaded it. See
     * <a href="http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html">here</a> for more information.
     * 
     * <p>
     * Any dependencies are still loaded using the system class loader.
     * For instance, a JLabel returned by a method of the loaded class is the same JLabel
     * as the one loaded by the system class loader.
     * 
     * @param className The fully qualified name of the class to reload.
     * @return A new instance of the class.
     * @throws RuntimeException If an error occurs while reading the class file.
     * @throws AssertionError If the class could not be found.
     */
    public static Class<?> newInstanceOfClass(final String className) {
        ClassLoader loader = new SingleClassLoader(className);
        return loadClassWith(className, loader);
    }
    
    /**
     * Loads a new instance of the class in a new class loader.
     * 
     * Please see {@link #newInstanceOfClass(java.lang.String)}.
     */
    public static Class<?> newInstanceOfClass(Class<?> cls) {
        return newInstanceOfClass(cls.getName());
    }
    
    /**
     * Loads a class with the given class loader and gives user-friendly errors.
     * 
     * @param className The fully qualified name of the class to load.
     * @param loader The class loader.
     * @return The class as loaded by the given class loader.
     * @throws RuntimeException If an error occurs while reading the class file.
     * @throws AssertionError If the class could not be found.
     */
    public static Class<?> loadClassWith(String className, ClassLoader loader) {
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            if (className.contains(".")) {
                throw new AssertionError(tr("class_not_found_pkg", className));
            } else {
                throw new AssertionError(tr("class_not_found", className));
            }
        }
    }
    
    /**
     * Finds a public constructor with the specified argument list.
     * 
     * @param <T> The type whose constructor to look for.
     * @param cls The class whose constructor to look for.
     * @param paramTypes The expected types of the parameters.
     * @return The constructor reflection object. Never null.
     * @throws AssertionError If the constructor could not be found.
     */
    public static <T> Constructor<T> requireConstructor(Class<T> cls, Class<?> ... paramTypes) {
        return requireConstructor(PUBLIC, cls, paramTypes);
    }
    
    /**
     * Finds a constructor with the specified access modifier and argument list.
     * 
     * @param <T> The type whose constructor to look for.
     * @param expectedAccess One or more (OR-ed) of PUBLIC, PROTECTED, PRIVATE or PACKAGE_PRIVATE to allow, or null to not check.
     * @param cls The class whose constructor to look for.
     * @param paramTypes The expected types of the parameters.
     * @return The constructor reflection object. Never null.
     * @throws AssertionError If the constructor could not be found.
     */
    public static <T> Constructor<T> requireConstructor(Integer expectedAccess, Class<T> cls, Class<?> ... paramTypes) {
        Constructor<T> ctor;
        try {
            ctor = cls.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(tr("ctor_missing", niceMethodSignature(cls.getSimpleName(), paramTypes)));
        } catch (SecurityException ex) {
            throw new AssertionError(tr("ctor_inaccessible", niceMethodSignature(cls.getSimpleName(), paramTypes)));
        }
        
        if (!isExpectedAccess(expectedAccess, ctor.getModifiers())) {
            throw new AssertionError(tr("ctor_wrong_access", niceConstructorSignature(ctor), accessModifiersToString(expectedAccess)));
        }
        
        return ctor;
    }
    
    /**
     * Finds a public method with the specified argument list.
     * 
     * <p>
     * This does not assert anything about the method's staticness or return type.
     * Other variants of this method do.
     * 
     * @param cls The class whose method to look for.
     * @param name The name of the method.
     * @param params The expected types of the parameters.
     * @return The method reflection object. Never null.
     * @throws AssertionError If the method could not be found.
     */
    public static Method requireMethod(Class<?> cls, String name, Class<?>... params) {
        return requireMethod(PUBLIC, null, cls, null, name, params);
    }
    
    /**
     * Finds a public method with the specified return type and argument list.
     * 
     * @param cls The class whose method to look for.
     * @param returnType The expected return type, or null to not check.
     * @param name The name of the method.
     * @param params The expected types of the parameters.
     * @return The method reflection object. Never null.
     * @throws AssertionError If the method could not be found.
     */
    public static Method requireMethod(Class<?> cls, Class<?> returnType, String name, Class<?>... params) {
        return requireMethod(PUBLIC, null, cls, returnType, name, params);
    }
    
    /**
     * Finds a public method with the specified staticness, return type and argument list.
     * 
     * @param expectStatic Whether the method must be static or non-static, or null to not check.
     * @param cls The class whose method to look for.
     * @param returnType The expected return type, or null to not check.
     * @param name The name of the method.
     * @param params The expected types of the parameters.
     * @return The method reflection object. Never null.
     * @throws AssertionError If the method could not be found.
     */
    public static Method requireMethod(Boolean expectStatic, Class<?> cls, Class<?> returnType, String name, Class<?>... params) {
        return requireMethod(PUBLIC, expectStatic, cls, returnType, name, params);
    }
    
    /**
     * Finds a method with the specified access modifiers, staticness, return type and argument list.
     * 
     * <p>
     * If a non-public method is found then it is made accessible.
     * 
     * <p>
     * This is the most generic variant of {@code requireMethod}
     * that all other variants call internall.y
     * 
     * @param expectedAccess One or more (OR-ed) of PUBLIC, PROTECTED, PRIVATE or PACKAGE_PRIVATE to allow, or null to not check.
     * @param expectStatic Whether the method must be static or non-static, or null to not check.
     * @param cls The class whose method to look for.
     * @param returnType The expected return type, or null to not check.
     * @param name The name of the method.
     * @param params The expected types of the parameters.
     * @return The method reflection object. Never null.
     * @throws AssertionError If the method could not be found.
     */
    public static Method requireMethod(Integer expectedAccess, Boolean expectStatic, Class<?> cls, Class<?> returnType, String name, Class<?>... params) {
        Method m;
        try {
            m = getDeclaredMethodInInheritanceTree(cls, name, params);
            m.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(tr("method_missing", niceMethodSignature(name, params), cls));
        } catch (SecurityException ex) {
            throw new AssertionError(tr("method_inaccessible", niceMethodSignature(name, params), cls));
        }
        
        if (returnType != null) {
            if (!m.getReturnType().equals(returnType)) {
                throw new AssertionError(tr("method_wrong_return_type", niceMethodSignature(returnType, name, params), cls));
            }
        }
        
        if (expectStatic != null) {
            boolean isStatic = ((m.getModifiers() & Modifier.STATIC) != 0);
            if (isStatic && !expectStatic) {
                throw new AssertionError(tr("method_should_not_be_static", niceMethodSignature(returnType, name, params), cls));
            } else if (!isStatic && expectStatic) {
                throw new AssertionError(tr("method_should_be_static", niceMethodSignature(returnType, name, params), cls));
            }
        }
        
        if (!isExpectedAccess(expectedAccess, m.getModifiers())) {
            throw new AssertionError(tr("method_wrong_access", niceMethodSignature(returnType, name, params), cls, accessModifiersToString(expectedAccess)));
        }
        
        return m;
    }
    
    /**
     * Like {@code Class.getMethod()} but also finds non-public methods.
     */
    private static Method getDeclaredMethodInInheritanceTree(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
        while (cls != null) {
            try {
                return cls.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ex) {
            }
            cls = cls.getSuperclass();
        }
        throw new NoSuchMethodException("Method " + niceMethodSignature(name, params) + " not found");
    }
    
    /**
     * Converts an access modifier ({@link #PUBLIC}, {@link #PROTECTED}, {@link #PRIVATE} and {@link #PACKAGE_PRIVATE}) to a lowercase string.
     * 
     * <p>
     * If more than one access modifier is given (OR-ed together), then returns their lowercase names separated by slashes.
     * If the set of OR-ed access modifiers is empty, returns an empty string.
     */
    public static String accessModifiersToString(int accessMod) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ALL_ACCESS_MODIFIERS.length; ++i) {
            if ((accessMod & ALL_ACCESS_MODIFIERS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append("/");
                }
                sb.append(ALL_ACCESS_MODIFIERS_STR[i]);
            }
        }
        
        return sb.toString();
    }
    
    private static boolean isExpectedAccess(Integer expectedAccess, int modifiers) {
        if (expectedAccess != null) {
            boolean accessOk = false;
            if ((expectedAccess & PACKAGE_PRIVATE) != 0 && isPackagePrivate(modifiers)) {
                accessOk = true;
            }
            expectedAccess = expectedAccess & ~PACKAGE_PRIVATE;
            if ((modifiers & expectedAccess) != 0) {
                accessOk = true;
            }
            return accessOk;
        } else {
            return true;
        }
    }
    
    private static boolean isPackagePrivate(int accessMod) {
        return !Modifier.isPublic(accessMod) &&
                !Modifier.isProtected(accessMod) &&
                !Modifier.isPrivate(accessMod);
    }

    /**
     * Returns a human-friendly representation of a method signature.
     * 
     * @param m The method.
     * @return A human-readable return type, name and parameter list of the method.
     */
    public static String niceMethodSignature(Method m) {
        return niceMethodSignature(m.getReturnType(), m.getName(), m.getParameterTypes());
    }
    
    /**
     * Returns a human-friendly representation of a constructor signature.
     * 
     * @param c The constructor.
     * @return A human-readable name and parameter list of the constructor.
     */
    public static String niceConstructorSignature(Constructor c) {
        return niceMethodSignature(c.getName(), c.getParameterTypes());
    }

    /**
     * Returns a human-friendly representation of a method signature.
     * 
     * @param methodName The name of the method.
     * @param paramTypes The method's parameter types.
     * @return A human-readable name and parameter list of the method.
     */
    public static String niceMethodSignature(String methodName, Class<?>... paramTypes) {
        String result = methodName;
        result += "(";
        if (paramTypes.length > 0) {
            for (int i = 0; i < paramTypes.length - 1; ++i) {
                result += paramTypes[i].getSimpleName() + ", ";
            }
            result += paramTypes[paramTypes.length - 1].getSimpleName();
        }
        result += ")";
        return result;
    }
    
    /**
     * Returns a human-friendly representation of a method signature.
     * 
     * @param returnType The return type of the method.
     * @param methodName The name of the method.
     * @param paramTypes The method's parameter types.
     * @return A human-readable return type, name and parameter list of the method.
     */
    public static String niceMethodSignature(Class<?> returnType, String methodName, Class<?>... paramTypes) {
        StringBuilder sb = new StringBuilder();
        if (returnType != null) {
            sb.append(returnType.getSimpleName()).append(' ');
        }
        sb.append(methodName);
        sb.append("(");
        if (paramTypes.length > 0) {
            for (int i = 0; i < paramTypes.length - 1; ++i) {
                sb.append(paramTypes[i].getSimpleName()).append(", ");
            }
            sb.append(paramTypes[paramTypes.length - 1].getSimpleName());
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Calls a constructor and passes errors through.
     * 
     * <p>
     * Inability to get at the constructor e.g. due to incorrect
     * parameter count or types results in an AssertionError while
     * an exception in the constructor is passed through directly
     * (unwrapped from InvocationTargetException).
     * 
     * @param <T> The type of object to construct.
     * @param ctor The constructor to invoke.
     * @param params The parameters to pass.
     * @return The constructed object.
     * @throws AssertionError If the constructor could not be called with the given parameters.
     * @throws Throwable An exception that occurred in the constructor is thrown directly.
     */
    public static <T> T invokeConstructor(Constructor<T> ctor, Object... params) throws Throwable {
        try {
            return ctor.newInstance(params);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(tr("ctor_inaccessible", niceConstructorSignature(ctor)));
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(tr("ctor_incorrect_params", niceConstructorSignature(ctor)));
        } catch (InstantiationException ex) {
            throw new AssertionError(tr("ctor_abstract", ctor.getDeclaringClass().getSimpleName()));
        } catch (ExceptionInInitializerError ex) {
            throw ex.getCause();
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
    
    /**
     * Calls a method and passes errors through.
     * 
     * <p>
     * Inability to get at the method e.g. due to incorrect
     * parameter count or types results in an AssertionError while
     * an exception in the method is passed through directly
     * (unwrapped from InvocationTargetException).
     * 
     * @param <T> The expected return type.
     * @param retType The expected return type. Pass {@code Void.TYPE} for void.
     * @param method The method to invoke.
     * @param self The <tt>this</tt> parameter for the method. Use null for static methods.
     * @param params The parameters to pass.
     * @return The return value.
     * @throws AssertionError If the method could not be called with the given parameters.
     * @throws Throwable An exception that occurred in the constructor is thrown directly.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Class<T> retType, Method method, Object self, Object... params) throws Throwable {
        Class<?> cls = method.getDeclaringClass();
        try {
            Object ret = method.invoke(self, params);
            if (retType == Void.TYPE) {
                if (ret != null) {
                    throw new AssertionError(tr("method_should_be_void", niceMethodSignature(method), cls));
                }
                return null;
            } else if (ret == null || primitiveTypeToObjectType(retType).isInstance(ret)) {
                return (T) ret;
            } else {
                throw new AssertionError(tr("method_wrong_return_type", niceMethodSignature(method), cls));
            }
        } catch (IllegalAccessException ex) {
            throw new AssertionError(tr("method_inaccessible", niceMethodSignature(method), cls));
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(tr("method_incorrect_params", niceMethodSignature(method), cls));
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
    
    /**
     * Creates an AssertionError with a helpful error message describing the original exception.
     * 
     * <p>
     * The new exception's error message looks roughly like this:
     * {@link "ExceptionType: exception msg, in call someMethod(arg1, arg2, ...). Custom msg (if any)."}
     * The message is subject to localization.
     * 
     * <p>
     * The new exception gets the same stack trace and cause as the original exception.
     * 
     * @param origEx The original exception.
     * @param methodName The name of the method that was called.
     * @param params The parameters that were given.
     * @param customMsg An optional extra message to add to the error.
     * @return A new exception with the same class and stack trace but a different 
     */
    public static AssertionError getNiceException(Throwable origEx, String methodName, Object[] params, String customMsg) {
        String origExAndMsg;
        if (origEx.getMessage() == null) {
            origExAndMsg = origEx.getClass().getSimpleName();
        } else {
            origExAndMsg = origEx.getClass().getSimpleName() + ": " + stripPunctuation(origEx.getMessage());
        }
        String callDesc = describeMethodCall(methodName, params);
        
        String newMsg;
        if (customMsg != null && !customMsg.isEmpty()) {
            newMsg = tr("exception_with_custom_msg",
                    origExAndMsg,
                    callDesc,
                    customMsg);
        } else {
            newMsg = tr("exception_without_custom_msg",
                    origExAndMsg,
                    callDesc);
        }
        
        AssertionError newEx = new AssertionError(newMsg);
        newEx.setStackTrace(origEx.getStackTrace());
        newEx.initCause(origEx.getCause());
        return newEx;
    }
    
    private static String describeMethodCall(String methodName, Object[] params) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append("(");
        for (int i = 0; i < params.length - 1; ++i) {
            sb.append(describeArg(params[i])).append(", ");
        }
        if (params.length > 0) {
            sb.append(describeArg(params[params.length - 1]));
        }
        sb.append(")");
        return sb.toString();
    }
    
    private static String describeArg(Object arg) {
        int maxLength = 50;
        String result;
        try {
            result = arg.toString();
        } catch (Exception ex) {
            result = "<error in .toString() of arg>"; // TODO: localize
        }
        
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength) + "...";
        }
        
        if (arg instanceof String) {
            result = '"' + result + '"';
        }
        
        return result;
    }
    
    private static String stripPunctuation(String s) {
        s = s.trim();
        if (s.isEmpty()) {
            return s;
        }
        
        char lastChar = s.charAt(s.length() - 1);
        if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }
    
    /**
     * Converts a class object representing a primitive type like
     * {@code Integer.TYPE} to the corresponding object type like
     * {@code Integer.class}
     * 
     * @param cls The class representing a primitive type.
     * @return The corresponding object type, or cls itself if cls was not a primitive type.
     */
    public static Class<?> primitiveTypeToObjectType(Class<?> cls) {
        if (cls == Integer.TYPE) {
            return Integer.class;
        } else if (cls == Long.TYPE) {
            return Float.class;
        } else if (cls == Short.TYPE) {
            return Short.class;
        } else if (cls == Byte.TYPE) {
            return Byte.class;
        } else if (cls == Boolean.TYPE) {
            return Boolean.class;
        } else if (cls == Float.TYPE) {
            return Float.class;
        } else if (cls == Double.TYPE) {
            return Double.class;
        } else if (cls == Character.TYPE) {
            return Character.class;
        } else {
            assert !cls.isPrimitive();
            return cls;
        }
    }
}
