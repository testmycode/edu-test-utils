package fi.helsinki.cs.tmc.edutestutils;

import fi.helsinki.cs.tmc.edutestutils.ReflectionUtilsNewClassInstanceTest.MainTestSubject;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectionUtilsNewClassInstanceTest {
    public static int timesLoaded = 0;
    public static class TestSubject {
        static {
            timesLoaded += 1;
        }
        
        public static void doNothing() {
        }
    }
    
    public static boolean mainInitsHaveRun = false;
    public static String[] latestArgs = null;
    public static class MainTestSubject {
        static {
            mainInitsHaveRun = true;
        }
        public static void main(String[] args) {
            latestArgs = args;
        }
    }
    
    public static int dependeeInitCount = 0;
    public static class Dependee {
        static {
            dependeeInitCount += 1;
        }
    }
    public static class Depender {
        public static Dependee dep = new Dependee();
    }
    
    @Test
    public void reloadsGivenClass() throws Exception {
        String className = TestSubject.class.getName();
        timesLoaded = 0;
        
        Class<?> cls1 = ReflectionUtils.newInstanceOfClass(className);
        assertEquals(0, timesLoaded); // Not yet initialized
        cls1.getMethod("doNothing").invoke(null); // Initializes the class
        assertEquals(1, timesLoaded);
        assertNotSame(TestSubject.class, cls1);
        
        Class<?> cls2 = ReflectionUtils.newInstanceOfClass(className);
        assertEquals(1, timesLoaded);
        cls2.newInstance(); // Initializes the class
        assertEquals(2, timesLoaded);
        
        assertNotSame(TestSubject.class, cls2);
        assertNotSame(cls1, cls2);
        
        assertFalse(cls1.newInstance() instanceof TestSubject);
    }
    
    @Test
    public void canRerunMainMethodAsInExample() throws Throwable {
        Class<?> reloadedMain = ReflectionUtils.newInstanceOfClass(MainTestSubject.class.getName());
        assertFalse(mainInitsHaveRun);
        Method mainMethod = ReflectionUtils.requireMethod(reloadedMain, "main", String[].class);
        assertFalse(mainInitsHaveRun);
        ReflectionUtils.invokeMethod(void.class, mainMethod, null, (Object)new String[] {"one", "two"});
        assertTrue(mainInitsHaveRun);
        assertArrayEquals(new String[] { "one", "two" }, latestArgs);
    }
    
    @Test
    public void doesNotReloadsDependentClasses() throws Throwable {
        assertEquals(0, dependeeInitCount);
        new Dependee();
        assertEquals(1, dependeeInitCount);
        
        Class<?> depender = ReflectionUtils.newInstanceOfClass(Depender.class.getName());
        depender.newInstance();
        Dependee dep = (Dependee)depender.getField("dep").get(null);
        assertEquals(1, dependeeInitCount);
    }
    
    @Test(expected=AssertionError.class)
    public void throwsAnAssertionErrorIfTheClassCannotBeFound() throws Throwable {
        ReflectionUtils.newInstanceOfClass("Nonexistent");
    }
}
