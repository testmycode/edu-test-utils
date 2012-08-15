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
        private TestSubject(EmptyClass x) {
            this.x = 1;
        }
        TestSubject(EmptyClass x, EmptyClass y) {
            this.x = 2;
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
        public Object returnsNull() {
            return null;
        }
        private int privateMethod() {
            return 42;
        }
        int packagePrivateMethod() {
            return 42;
        }
    }
    
    @After
    public void resetLocale() {
        EduTestUtilsDefaultLocale.reset();
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
    public void requireConstructorSucceedsWhenAccessModifiersMatch() {
        ReflectionUtils.requireConstructor(ReflectionUtils.PUBLIC | ReflectionUtils.PRIVATE, TestSubject.class, int.class);
        ReflectionUtils.requireConstructor(ReflectionUtils.PUBLIC | ReflectionUtils.PACKAGE_PRIVATE, TestSubject.class, int.class);
        ReflectionUtils.requireConstructor(ReflectionUtils.PUBLIC, TestSubject.class, int.class);
        ReflectionUtils.requireConstructor(ReflectionUtils.PRIVATE, TestSubject.class, EmptyClass.class);
        ReflectionUtils.requireConstructor(ReflectionUtils.PRIVATE | ReflectionUtils.PUBLIC, TestSubject.class, EmptyClass.class);
    }

    @Test(expected=AssertionError.class)
    public void requireConstructorFailsWhenAccessModifiersDontMatch() {
        ReflectionUtils.requireConstructor(ReflectionUtils.PRIVATE, TestSubject.class, int.class);
    }

    @Test
    public void requireConstructorSucceedsWhenDontCareAboutAccessModifiers() {
        ReflectionUtils.requireConstructor((Integer)null, TestSubject.class, int.class);
    }

    @Test
    public void requireConstructorSucceedsWhenAccessModifiersMatchPackagePrivate() {
        ReflectionUtils.requireConstructor(ReflectionUtils.PACKAGE_PRIVATE, TestSubject.class, EmptyClass.class, EmptyClass.class);
    }

    @Test(expected=AssertionError.class)
    public void requireConstructorFailsWhenAccessModifiersDontMatchPackagePrivate() {
        ReflectionUtils.requireConstructor(ReflectionUtils.PACKAGE_PRIVATE, TestSubject.class, EmptyClass.class);
    }

    @Test
    public void canRequireAndCallPrivateConstructors() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(ReflectionUtils.PRIVATE, TestSubject.class, EmptyClass.class);
        assertTrue(ctor.newInstance(new EmptyClass()) instanceof TestSubject);
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
    public void requireMethodSucceedsWhenAccessModifiersMatch() {
        ReflectionUtils.requireMethod(ReflectionUtils.PUBLIC | ReflectionUtils.PRIVATE, false, TestSubject.class, int.class, "getX");
        ReflectionUtils.requireMethod(ReflectionUtils.PUBLIC | ReflectionUtils.PACKAGE_PRIVATE, false, TestSubject.class, int.class, "getX");
        ReflectionUtils.requireMethod(ReflectionUtils.PUBLIC, false, TestSubject.class, int.class, "getX");
        ReflectionUtils.requireMethod(ReflectionUtils.PRIVATE, false, TestSubject.class, int.class, "privateMethod");
        ReflectionUtils.requireMethod(ReflectionUtils.PRIVATE | ReflectionUtils.PUBLIC, false, TestSubject.class, int.class, "privateMethod");
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenAccessModifiersDontMatch() {
        ReflectionUtils.requireMethod(ReflectionUtils.PRIVATE, false, TestSubject.class, int.class, "getX");
    }
    
    @Test
    public void requireMethodSucceedsWhenDontCareAboutAccessModifiers() {
        ReflectionUtils.requireMethod(null, false, TestSubject.class, int.class, "getX");
    }
    
    @Test
    public void requireMethodSucceedsWhenAccessModifiersMatchPackagePrivate() {
        ReflectionUtils.requireMethod(ReflectionUtils.PACKAGE_PRIVATE, false, TestSubject.class, int.class, "packagePrivateMethod");
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodFailsWhenAccessModifiersDontMatchPackagePrivate() {
        ReflectionUtils.requireMethod(ReflectionUtils.PACKAGE_PRIVATE, false, TestSubject.class, int.class, "getX");
    }
    
    @Test
    public void canRequireAndCallPrivateMethods() throws Throwable {
        Method m = ReflectionUtils.requireMethod(ReflectionUtils.PRIVATE, false, TestSubject.class, int.class, "privateMethod");
        TestSubject obj = new TestSubject();
        int ret = (Integer)m.invoke(obj);
        assertEquals(42, ret);
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
    public void returningNullWorks() throws Throwable {
        Constructor<?> ctor = ReflectionUtils.requireConstructor(TestSubject.class);
        Object obj = ReflectionUtils.invokeConstructor(ctor);
        Method m = ReflectionUtils.requireMethod(TestSubject.class, "returnsNull");
        assertNull(ReflectionUtils.invokeMethod(Object.class, m, obj));
    }
    
    @Test
    public void packagePrivateClassAndMethodsWork() throws Throwable {
        Class<?> cls = ReflectionUtils.findClass("fi.helsinki.cs.tmc.edutestutils.testpkg.PkgPrivateClass");
        Constructor<?> ctor = ReflectionUtils.requireConstructor(cls, int.class);
        Object obj = ReflectionUtils.invokeConstructor(ctor, 3);
        Method m = ReflectionUtils.requireMethod(cls, "getX");
        int x = ReflectionUtils.invokeMethod(int.class, m, obj);
        assertEquals(3, x);
    }
    
    @Test(expected=AssertionError.class)
    public void requireClassAccessThrowsIfTheClassAccessModifierDoesntMatch() {
        Class<?> cls = ReflectionUtils.findClass("fi.helsinki.cs.tmc.edutestutils.testpkg.PkgPrivateClass");
        ReflectionUtils.requireClassAccess(cls, ReflectionUtils.PUBLIC | ReflectionUtils.PROTECTED | ReflectionUtils.PRIVATE);
    }
    
    @Test
    public void requireClassAccessDoesntDoesntThrowIfTheClassAccessModifierMatches() {
        Class<?> cls = ReflectionUtils.findClass("fi.helsinki.cs.tmc.edutestutils.testpkg.PkgPrivateClass");
        ReflectionUtils.requireClassAccess(cls, ReflectionUtils.PACKAGE_PRIVATE);
    }

    @Test
    public void localizedErrorMessages() throws Throwable {
        EduTestUtilsDefaultLocale.set(new Locale("fi"));
        
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
