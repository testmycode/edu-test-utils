package fi.helsinki.cs.tmc.edutestutils;

import java.util.ArrayList;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EduTestUtilsDefaultLocaleTest {
    
    private String prop;
    
    @Before
    public void saveProperty() {
        prop = System.getProperty(EduTestUtilsDefaultLocale.PROPERTY_NAME);
    }
    
    @After
    public void resetLocale() {
        if (prop == null) {
            System.getProperties().remove(EduTestUtilsDefaultLocale.PROPERTY_NAME);
        } else {
            System.setProperty(EduTestUtilsDefaultLocale.PROPERTY_NAME, prop);
        }
        EduTestUtilsDefaultLocale.reset();
    }
    
    @Test
    public void readingLocaleFromEnv() {
        System.setProperty(EduTestUtilsDefaultLocale.PROPERTY_NAME, "en_GB");
        EduTestUtilsDefaultLocale.reset();
        assertEquals("eng", EduTestUtilsDefaultLocale.get().getISO3Language());
        
        System.setProperty(EduTestUtilsDefaultLocale.PROPERTY_NAME, "fi");
        EduTestUtilsDefaultLocale.reset();
        assertEquals("fin", EduTestUtilsDefaultLocale.get().getISO3Language());
    }
    
    @Test
    public void defaultingToSystemLocale() {
        System.getProperties().remove(EduTestUtilsDefaultLocale.PROPERTY_NAME);
        assertEquals(Locale.getDefault(), EduTestUtilsDefaultLocale.get());
    }
    
    @Test
    public void listeners() {
        final ArrayList<Locale> receivedLocales = new ArrayList<Locale>();
        EduTestUtilsDefaultLocale.Listener listener = new EduTestUtilsDefaultLocale.Listener() {
            public void eduTestUtilsLocaleChanged(Locale newLocale) {
                receivedLocales.add(newLocale);
            }
        };
        
        EduTestUtilsDefaultLocale.addListener(listener);
        try {
            Locale fi = new Locale("fi");
            EduTestUtilsDefaultLocale.set(new Locale("fi"));
            EduTestUtilsDefaultLocale.reset();
            Locale def = EduTestUtilsDefaultLocale.get();
            
            assertArrayEquals(new Object[] {fi, def}, receivedLocales.toArray());
        } finally {
            EduTestUtilsDefaultLocale.removeListener(listener);
        }
    }
}
