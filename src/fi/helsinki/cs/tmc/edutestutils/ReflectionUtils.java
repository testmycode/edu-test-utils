package fi.helsinki.cs.tmc.edutestutils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Helpers for testing a student's class entirely through reflection
 * with nice error messages for missing methods and other mistakes.
 * 
 * <p>
 * All methods throw AssertionError with a friendly error message on failure
 * (or {@link IllegalArgumentException} if used incorrectly),
 * so you can use them as if they were assertions.
 * 
 * <p>
 * NOTE: a common mistake is to forget the difference between {@code Integer.class} and
 * {@code Integer.TYPE}. The former refers to the object type {@code Integer} and the latter
 * to the primitive type {@code int}.
 */
public class ReflectionUtils {

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
     * @param className The fully qualified name of the class to reload.
     * @return A new instance of the class.
     * @throws RuntimeException If an error occurs while reading the class file.
     * @throws AssertionError If the class could not be found.
     */
    public static Class<?> newInstanceOfClass(final String className) {
        ClassLoader loader = new ClassLoader() {
            
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return loadClass(name, false);
            }
            
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(className)) {
                    return super.loadClass(name, resolve);
                }
                
                InputStream classIn = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
                if (classIn == null) {
                    throw new ClassNotFoundException(name);
                }
                classIn = new BufferedInputStream(classIn);
                
                try {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();

                    int data = classIn.read();
                    while (data != -1) {
                        buf.write(data);
                        data = classIn.read();
                    }
                    
                    byte[] classDef = buf.toByteArray();
                    return defineClass(name, classDef, 0, classDef.length);
                } catch (IOException e) {
                    throw new RuntimeException("Error reloading class " + name, e);
                } finally {
                    try {
                        classIn.close();
                    } catch (IOException e) {
                    }
                }
            }
        };
        
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
    
    private static Class<?> loadClassWith(String name, ClassLoader loader) {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            if (name.contains(".")) {
                throw new AssertionError("Could not find class `" + name + "`. Is it in the correct package?");
            } else {
                throw new AssertionError("Could not find class `" + name + "`.");
            }
        }
    }
    
    /**
     * Finds a constructor with the specified argument list.
     * 
     * @param <T> The type whose constructor to look for.
     * @param cls The class whose constructor to look for.
     * @param paramTypes The expected types of the parameters.
     * @return The constructor reflection object. Never null.
     * @throws AssertionError If the constructor could not be found.
     */
    public static <T> Constructor<T> requireConstructor(Class<T> cls, Class<?> ... paramTypes) {
        try {
            return cls.getConstructor(paramTypes);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("The constructor " + niceMethodSignature(cls.getSimpleName(), paramTypes) + " is missing.");
        } catch (SecurityException ex) {
            throw new AssertionError("The constructor " + niceMethodSignature(cls.getSimpleName(), paramTypes) + " could not be accessed. Is it public?");
        }
    }
    
    /**
     * Finds a public method with the specified argument list.
     * 
     * <p>
     * This does not assert anything about the return type, but
     * {@link #invokeMethod(Class, Method, Object, Object[])}
     * does.
     * 
     * @param cls The class whose method to look for.
     * @param name The name of the method.
     * @param params The expected types of the parameters.
     * @return The method reflection object. Never null.
     * @throws AssertionError If the method could not be found.
     */
    public static Method requireMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getMethod(name, params);
            if ((m.getModifiers() & Modifier.PUBLIC) == 0) {
                throw new SecurityException();
            }
            return m;
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Method " + niceMethodSignature(name, params) + " missing.");
        } catch (SecurityException ex) {
            throw new AssertionError("The method " + niceMethodSignature(name, params) + " could not be accessed. Is it public?");
        }
    }

    /**
     * Returns a human-friendly representation of a method signature.
     * 
     * @param m The method.
     * @return A human-readable name and parameter list of the method.
     */
    public static String niceMethodSignature(Method m) {
        return niceMethodSignature(m.getName(), m.getParameterTypes());
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
            throw new AssertionError("The constructor " + ctor.toGenericString() + " could not be accessed.");
        } catch (IllegalArgumentException ex) {
            throw new AssertionError("The constructor " + ctor.toGenericString() + " has an incorrect number of parameters.");
        } catch (InstantiationException ex) {
            throw new AssertionError("Cannot create an instance of " + ctor.getDeclaringClass().getSimpleName() + ".");
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
        try {
            Object ret = method.invoke(self, params);
            if (retType == Void.TYPE) {
                if (ret != null) {
                    throw new AssertionError("The method " + niceMethodSignature(method) + " should not return anything.");
                }
                return null;
            } else if (primitiveTypeToObjectType(retType).isInstance(ret)) {
                return (T) ret;
            } else {
                throw new AssertionError("The method " + niceMethodSignature(method) + " has an incorrect return type.");
            }
        } catch (IllegalAccessException ex) {
            throw new AssertionError("The method " + niceMethodSignature(method) + " could not be accessed.");
        } catch (IllegalArgumentException ex) {
            throw new AssertionError("The method " + niceMethodSignature(method) + " has an incorrect number of parameters.");
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
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
