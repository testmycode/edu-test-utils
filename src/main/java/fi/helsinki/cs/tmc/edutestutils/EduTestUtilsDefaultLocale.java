package fi.helsinki.cs.tmc.edutestutils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Returns either the default locale or a locale defined in a system property.
 * 
 * The locale affects the language of user-friendly error messages in
 * components like {@link ReflectionUtils}.
 * 
 * The default may be set in the system property {@code fi.helsinki.cs.tmc.edutestutils.defaultLocale}.
 * If it's not set, the system default is used.
 */
public class EduTestUtilsDefaultLocale {
    public static final String PROPERTY_NAME = "fi.helsinki.cs.tmc.edutestutils.defaultLocale";
    private static Locale locale;
    
    /**
     * Listens to changes in the default locale.
     * 
     * For internal use.
     */
    public static interface Listener {
        public void eduTestUtilsLocaleChanged(Locale newLocale);
    }
    
    private static ArrayList<Listener> listeners = new ArrayList<Listener>();
    
    static {
        reset();
    }
    
    private EduTestUtilsDefaultLocale() {
    }
    
    /**
     * Returns the current locale.
     * 
     * Never returns null.
     */
    public static Locale get() {
        return locale;
    }
    
    /**
     * Sets the locale globally.
     */
    public static void set(Locale newLocale) {
        locale = newLocale;
        notifyListeners(newLocale);
    }
    
    /**
     * Resets the locale to what it was initially.
     */
    public static void reset() {
        Locale newLocale = null;
        
        String envLocale = System.getProperty(PROPERTY_NAME);
        if (envLocale != null && !envLocale.isEmpty()) {
            try {
                newLocale = parseLocale(envLocale);
            } catch (Exception e) {
                System.err.println("Warning: invalid locale '" + envLocale + "' in " + PROPERTY_NAME + ": " + e.getMessage());
            }
        }
        
        if (newLocale == null) {
            newLocale = Locale.getDefault();
        }
        
        set(newLocale);
    }
    
    private static Locale parseLocale(String localeStr) {
        String[] parts = localeStr.split("_");
        switch (parts.length) {
            case 1: return new Locale(parts[0]);
            case 2: return new Locale(parts[0], parts[1]);
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            default: throw new IllegalArgumentException("Expected format: LANG[_COUNTRY[_VARIANT]]");
        }
    }
    
    /**
     * Registers a listener for when the locale changes.
     * 
     * For internal use.
     */
    public static void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener for when the locale changes.
     * 
     * For internal use.
     */
    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    
    private static void notifyListeners(Locale newLocale) {
        for (Listener listener : listeners) {
            listener.eduTestUtilsLocaleChanged(newLocale);
        }
    }
}
