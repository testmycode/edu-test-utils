package fi.helsinki.cs.tmc.edutestutils.classloaders;

/**
 * Loads a specific class itself and delegates others to its parent.
 */
public class SingleClassLoader extends OverridingClassLoader {
    /*
        In memory mode can be used to override the classloader when e.g. running
        the tests in in-memory mode. If you do so, make sure that you also
        override the classloader in ReflectionUtils as it is being used by
        fi.helsinki.cs.tmc.edutestutils.Reflex
    */
    public static boolean IN_MEMORY_MODE = false;
    public static ClassLoader IN_MEMORY_CLASSLOADER;
    private String nameOfClassToLoad;

    public SingleClassLoader(String nameOfClassToLoad) {
        this.nameOfClassToLoad = nameOfClassToLoad;
    }
    
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(IN_MEMORY_MODE) {
            return IN_MEMORY_CLASSLOADER.loadClass(name);
        }
        
        if (name.equals(nameOfClassToLoad)) {
            return super.loadClass(name, resolve);
        } else {
            return getParent().loadClass(name);
        }
    }
}
