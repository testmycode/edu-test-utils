package fi.helsinki.cs.tmc.edutestutils.classloaders;

/**
 * A class loader that prefers to load a class itself.
 * 
 * <p>
 * This delegates to the parent loader only if it fails to load the class itself.
 * 
 * <p>
 * Used alone, it loads classes and their dependencies into a new class space.
 * Classes in {@code java.*}, however, cannot be redefined due to a restriction
 * built into the JVM. They are loaded directly from the parent loader.
 */
public class OverridingClassLoader extends AbstractClassLoader {
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return getParent().loadClass(name);
        }
        
        Class<?> cls = findLoadedClass(name);
        try {
            if (cls == null) {
                cls = findClass(name);
            }
        } catch (ClassNotFoundException e) {
            return getParent().loadClass(name);
        }
        if (resolve) {
            resolveClass(cls);
        }
        return cls;
    }
}
