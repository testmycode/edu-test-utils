package fi.helsinki.cs.tmc.edutestutils.classloaders;

/**
 * Loads a specific class itself and delegates others to its parent.
 */
public class SingleClassLoader extends OverridingClassLoader {
    private String nameOfClassToLoad;

    public SingleClassLoader(String nameOfClassToLoad) {
        this.nameOfClassToLoad = nameOfClassToLoad;
    }
    
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equals(nameOfClassToLoad)) {
            return super.loadClass(name, resolve);
        } else {
            return getParent().loadClass(name);
        }
    }
}
