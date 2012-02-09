package fi.helsinki.cs.tmc.edutestutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectionUtilsTest {
    
    public static class EmptyClass {
    }
    
    public static class TestSubject {
        private int x;
        public TestSubject() {
        }
        public TestSubject(int x) {
            this.x = x;
        }
        public TestSubject(String x) {
            throw new IllegalStateException();
        }
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public void throwISE() {
            throw new IllegalStateException();
        }
        public static int staticMethod(int a, int b) {
            return a + b;
        }
    }
    
    @After
    public void resetLocale() {
        ReflectionUtils.setMsgLocale(null);
    }
    
    @Test
    public void findClassSearchesForAClassByFullyQualifiedName() {
        Class<?> cls = ReflectionUtils.findClass(ReflectionUtilsTest.class.getCanonicalName());
        assertEquals(ReflectionUtilsTest.class, cls);
    }
    
    @Test(expected=AssertionError.class)
    public void findClassFailsWhenTheClassCannotBeFound() {
        ReflectionUtils.findClass("bogus");
    }
    
    @Test
    public void requireConstructorFindsConstructorByParameterList() {
        assertNotNull(ReflectionUtils.requireConstructor(TestSubject.class, Integer.TYPE));
    }
    
    @Test
    public void requireConstructorFindsTheDefaultConstructor() {
        assertNotNull(ReflectionUtils.requireConstructor(EmptyClass.class));
    }

    @Test(expected=AssertionError.class)
    public void requireConstructorFailsWhenTheConstructorCannotBeFound() {
        ReflectionUtils.requireConstructor(TestSubject.class, Integer.class);
    }
    
    @Test
    public void requireMethodFindsMethodByParameterList() {
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "getX"));
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "setX", Integer.TYPE));
    }

    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenTheMethodCannotBeFound() {
        ReflectionUtils.requireMethod(TestSubject.class, "foo");
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenTheMethodHasTheWrongTypesOfParameters() {
        ReflectionUtils.requireMethod(TestSubject.class, "setX", Long.TYPE);
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenTheMethodsReturnTypeDoesntMatch() {
        ReflectionUtils.requireMethod(TestSubject.class, String.class, "getX");
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenNotStaticWhenShouldBe() {
        ReflectionUtils.requireMethod(true, TestSubject.class, String.class, "getX");
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenStaticWhenShouldNotBe() {
        ReflectionUtils.requireMethod(false, TestSubject.class, String.class, "staticMethod");
    }
    
    @Test
    public void niceMethodSignatureReturnsAHumanReadableMethodSignature() {
        String result = ReflectionUtils.niceMethodSignature("foo", Integer.TYPE, String.class, Method.class);
        assertEquals("foo(int, String, Method)", result);
    }
    
    @Test
    public void canInvokeConstructorsAndMethods() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(TestSubject.class);
        Object obj = ReflectionUtils.invokeConstructor(ctor);
        Method setX = ReflectionUtils.requireMethod(TestSubject.class, "setX", Integer.TYPE);
        Method getX = ReflectionUtils.requireMethod(TestSubject.class, "getX");
        ReflectionUtils.invokeMethod(Void.TYPE, setX, obj, 10);
        assertEquals(10, (int)ReflectionUtils.invokeMethod(Integer.TYPE, getX, obj));
    }
    
    @Test(expected=IllegalStateException.class)
    public void constructorInvokationPassesThroughErrors() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(TestSubject.class, String.class);
        ReflectionUtils.invokeConstructor(ctor, "xoo");
    }
    
    @Test(expected=IllegalStateException.class)
    public void methodInvokationPassesThroughErrors() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(TestSubject.class);
        Object obj = ReflectionUtils.invokeConstructor(ctor);
        Method m = ReflectionUtils.requireMethod(TestSubject.class, "throwISE");
        ReflectionUtils.invokeMethod(Void.TYPE, m, obj);
    }
    
    @Test
    public void localizedErrorMessages() throws Throwable {
        ReflectionUtils.setMsgLocale(new Locale("fi"));
        
        AssertionError ex = null;
        try {
            ReflectionUtils.findClass("Nonexistent");
        } catch (AssertionError e) {
            ex = e;
        }
        
        assertNotNull(ex);
        assertEquals("Luokkaa `Nonexistent` ei löytynyt.", ex.getMessage());
    }
    
}
