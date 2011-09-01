package fi.helsinki.cs.tmc.edutestutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
    }
    
    @Test
    public void findClassSearchesForAClassByFullyQualifiedName() {
        String thisPkg = this.getClass().getPackage().getName();
        Class<?> cls = ReflectionUtils.findClass(thisPkg + ".ReflectionUtilsTest");
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
        assertNotNull(ReflectionUtils.requireConstructor(TestSubject.class, Integer.class));
    }
    
    @Test
    public void requireMethodFindsMethodByParameterList() {
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "getX"));
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "setX", Integer.TYPE));
    }

    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenTheMethodCannotBeFound() {
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "foo"));
        assertNotNull(ReflectionUtils.requireMethod(TestSubject.class, "setX", Long.TYPE));
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
        Object obj = ReflectionUtils.invokeConstructor(ctor, "xoo");
    }
    
    @Test(expected=IllegalStateException.class)
    public void methodInvokationPassesThroughErrors() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(TestSubject.class);
        Object obj = ReflectionUtils.invokeConstructor(ctor);
        Method m = ReflectionUtils.requireMethod(TestSubject.class, "throwISE");
        ReflectionUtils.invokeMethod(Void.TYPE, m, obj);
    }
    
}
