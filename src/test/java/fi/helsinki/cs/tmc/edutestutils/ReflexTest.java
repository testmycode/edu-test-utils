package fi.helsinki.cs.tmc.edutestutils;

import fi.helsinki.cs.tmc.edutestutils.Reflex.ClassRef;
import fi.helsinki.cs.tmc.edutestutils.Reflex.MethodRef0;
import fi.helsinki.cs.tmc.edutestutils.Reflex.MethodRef1;
import java.util.Locale;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;

public class ReflexTest {
    
    public static class EmptyClass {
    }
    
    public static class Superclass {
    }
    
    public static class TestSubject extends Superclass {
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
        public void takeSelf(TestSubject ts) {
        }
    }
    
    @After
    public void resetLocale() {
        EduTestUtilsDefaultLocale.reset();
    }
    
    @Test
    public void findClassSearchesForAClassByFullyQualifiedName() {
        ClassRef<?> ref = Reflex.reflect(ReflexTest.class.getCanonicalName());
        assertEquals(ReflexTest.class, ref.getReferencedClass());
    }
    
    @Test(expected=AssertionError.class)
    public void reflectFailsWhenTheClassCannotBeFound() {
        Reflex.reflect("bogus");
    }
    
    @Test
    public void findsConstructorByParameterList() {
        assertTrue(Reflex.reflect(TestSubject.class).constructor().taking(int.class).exists());
    }
    
    @Test
    public void findsTheDefaultConstructor() {
        assertTrue(Reflex.reflect(EmptyClass.class).constructor().takingNoParams().exists());
    }
    
    @Test
    public void canTellIfAConstructorDoesntExist() {
        assertFalse(Reflex.reflect(TestSubject.class).constructor().taking(char.class).exists());
    }

    @Test(expected=AssertionError.class)
    public void failsWhenTheConstructorCannotBeFound() throws Throwable {
        Reflex.reflect(TestSubject.class).constructor().taking(char.class).invoke('x');
    }
    
    @Test
    public void requireMethodFindsMethodByParameterList() {
        assertTrue(Reflex.reflect(TestSubject.class).method(null, "getX").returning(int.class).takingNoParams().exists());
        assertTrue(Reflex.reflect(TestSubject.class).method(null, "setX").returningVoid().taking(int.class).exists());
    }
    
    @Test
    public void canTellAMethodDoesntExistBecauseTheReturnTypeDoesntMatch() {
        assertFalse(Reflex.reflect(TestSubject.class).method(null, "getX").returningVoid().takingNoParams().exists());
    }
    
    @Test
    public void canTellAMethodDoesntExistBecauseParametersDontMatch() {
        assertFalse(Reflex.reflect(TestSubject.class).method(null, "setX").returningVoid().taking(String.class).exists());
        assertFalse(Reflex.reflect(TestSubject.class).method(null, "setX").returningVoid().taking(int.class, int.class).exists());
        assertFalse(Reflex.reflect(TestSubject.class).method(null, "setX").returningVoid().takingNoParams().exists());
    }
    
    @Test
    public void canReturnAHumanReadableMethodSignature() {
        String result = Reflex.reflect(TestSubject.class).method(null, "setX").returningVoid().taking(int.class).signature();
        assertEquals("void setX(int)", result);
    }
    
    @Test
    public void canInvokeConstructorsAndMethods() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        MethodRef0<TestSubject, TestSubject> ctor = testSubject.constructor().takingNoParams();
        TestSubject obj = ctor.invoke();
        MethodRef1<TestSubject, Void, Integer> setX = testSubject.method(obj, "setX").returningVoid().taking(int.class);
        MethodRef0<TestSubject, Integer> getX = testSubject.method(obj, "getX").returning(int.class).takingNoParams();
        setX.invoke(10);
        assertEquals(10, getX.invoke().intValue());
    }
    
    @Test
    public void canInvokeConstructorsAndMethodsOnUntypedClass() throws Throwable {
        ClassRef<Object> testSubject = Reflex.reflect(ReflexTest.class.getCanonicalName() + "$TestSubject");
        MethodRef0<Object, Object> ctor = testSubject.constructor().takingNoParams();
        Object obj = ctor.invoke();
        MethodRef1<Object, Void, Integer> setX = testSubject.method(obj, "setX").returningVoid().taking(int.class);
        MethodRef0<Object, Integer> getX = testSubject.method(obj, "getX").returning(int.class).takingNoParams();
        setX.invoke(10);
        assertEquals(10, getX.invoke().intValue());
    }
    
    @Test
    public void canInvokeOnAThisParameterProvidedLater() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        MethodRef0<TestSubject, TestSubject> ctor = testSubject.constructor().takingNoParams();
        TestSubject obj = ctor.invoke();
        MethodRef1<TestSubject, Void, Integer> setX = testSubject.method("setX").returningVoid().taking(int.class);
        MethodRef0<TestSubject, Integer> getX = testSubject.method("getX").returning(int.class).takingNoParams();
        setX.invokeOn(obj, 10);
        assertEquals(10, getX.invokeOn(obj).intValue());
    }
    
    @Test(expected=IllegalStateException.class)
    public void constructorInvokationPassesThroughErrors() throws Throwable {
        MethodRef1<TestSubject, TestSubject, String> ctor = Reflex.reflect(TestSubject.class).constructor().taking(String.class);
        ctor.invoke("xoo");
    }
    
    @Test(expected=IllegalStateException.class)
    public void methodInvokationPassesThroughErrors() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        TestSubject obj = testSubject.constructor().taking(String.class).invoke("foo");
        testSubject.method(obj, "throwISE").returningVoid().takingNoParams().invoke();
    }
    
    @Test
    public void worksWithStaticMethods() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        int result = testSubject.staticMethod("staticMethod").returning(int.class).taking(int.class, int.class).invoke(3, 4);
        assertEquals(7, result);
    }
    
    @Test
    public void methodExistanceCheckRespectsStaticness() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        
        assertTrue(testSubject.staticMethod("staticMethod").returning(int.class).taking(int.class, int.class).exists());
        assertFalse(testSubject.method("staticMethod").returning(int.class).taking(int.class, int.class).exists());
        
        assertTrue(testSubject.method("getX").returning(int.class).takingNoParams().exists());
        assertFalse(testSubject.staticMethod("getX").returning(int.class).takingNoParams().exists());
    }
    
    @Test
    public void requireMethodWithAccessModifiersSuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).method("getX").returning(int.class).takingNoParams().requirePublic();
        Reflex.reflect(TestSubject.class).method("privateMethod").returning(int.class).takingNoParams().requirePrivate();
        Reflex.reflect(TestSubject.class).method("privateMethod").returning(int.class).takingNoParams().requireExists();
    }
    
    @Test(expected=AssertionError.class)
    public void requireMethodWithAccessModifiersUnsuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).method("getX").returning(int.class).takingNoParams().requirePrivate();
    }
    
    @Test
    public void requireStaticMethodWithAccessModifiersSuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).staticMethod("staticMethod").returning(int.class).taking(int.class, int.class).requirePublic();
    }
    
    @Test(expected=AssertionError.class)
    public void requireStaticMethodWithAccessModifiersUnsuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).staticMethod("staticMethod").returning(int.class).taking(int.class, int.class).requirePrivate();
    }
    
    @Test
    public void requireConstructorWithAccessModifiersSuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).constructor().taking(int.class).requirePublic();
        Reflex.reflect(TestSubject.class).constructor().taking(EmptyClass.class).requirePrivate();
        Reflex.reflect(TestSubject.class).constructor().taking(EmptyClass.class, EmptyClass.class).requirePackagePrivate();
    }
    
    @Test(expected=AssertionError.class)
    public void requireConstructorWithAccessModifiersUnsuccessfully() throws Throwable {
        Reflex.reflect(TestSubject.class).constructor().taking(int.class).requirePrivate();
    }
    
    @Test
    public void readingAccessModifiers() throws Throwable {
        MethodRef0<TestSubject, Integer> mr = Reflex.reflect(TestSubject.class).method("privateMethod").returning(int.class).takingNoParams();
        assertTrue(mr.exists());
        assertTrue(mr.isPrivate());
        assertFalse(mr.isPublic());
        assertFalse(mr.isProtected());
        assertFalse(mr.isPackagePrivate());
    }
    
    @Test
    public void callingPrivateMethods() throws Throwable {
        MethodRef0<TestSubject, Integer> mr = Reflex.reflect(TestSubject.class).method("privateMethod").returning(int.class).takingNoParams();
        int result = mr.invokeOn(new TestSubject());
        assertEquals(42, result);
    }
    
    @Test
    public void methodSignature() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        assertEquals("int getX()", testSubject.method("getX").returning(int.class).takingNoParams().signature());
        assertEquals("void setX(int)", testSubject.method("setX").returning(void.class).taking(int.class).signature());
        assertEquals("static int staticMethod(int, int)", testSubject.staticMethod("staticMethod").returning(int.class).taking(int.class, int.class).signature());
    }
    
    @Test
    public void constructorSignature() throws Throwable {
        ClassRef<TestSubject> testSubject = Reflex.reflect(TestSubject.class);
        assertEquals("TestSubject(int)", testSubject.constructor().taking(int.class).signature());
    }
    
    @Test
    public void inheritsCheck() throws Throwable {
        ClassRef<TestSubject> ts = Reflex.reflect(TestSubject.class);
        ClassRef<Superclass> sc = Reflex.reflect(Superclass.class);
        assertTrue(ts.inherits(ts));
        assertTrue(ts.inherits(sc));
        assertFalse(sc.inherits(ts));
    }
    
    @Test
    public void typeParameterToRepresentDynamicallyLoadedClassPattern() throws Throwable {
        class TestCase<T> {
            public void run() throws Throwable {
                ClassRef<T> cr = Reflex.reflect(ReflexTest.class.getCanonicalName() + "$TestSubject");
                T obj = cr.constructor().takingNoParams().invoke();
                MethodRef1<T, Void, T> mr = cr.method(obj, "takeSelf").returningVoid().taking(cr.getReferencedClass());
                mr.invoke(obj);
            }
        }
        new TestCase().run();
    }
    
    @Test
    public void niceErrorsFromConstructors() throws Throwable {
        try {
            Reflex.reflect(TestSubject.class)
                    .constructor()
                    .taking(String.class)
                    .withNiceError("Too bad.")
                    .invoke("hi");
        } catch (AssertionError ex) {
            assertEquals("IllegalStateException, in call TestSubject(\"hi\"). Too bad.", ex.getMessage());
            return;
        }
        fail("Exception expected");
    }
    
    @Test
    public void niceErrorsFromMethods() throws Throwable {
        try {
            TestSubject ts = new TestSubject();
            Reflex.reflect(TestSubject.class)
                    .method(ts, "throwISE")
                    .returningVoid()
                    .takingNoParams()
                    .withNiceError("So sad.")
                    .invoke();
        } catch (AssertionError ex) {
            assertEquals("IllegalStateException, in call throwISE(). So sad.", ex.getMessage());
            return;
        }
        fail("Exception expected");
    }
    
    @Test
    public void localizedErrorMessages() throws Throwable {
        EduTestUtilsDefaultLocale.set(new Locale("fi"));
        
        AssertionError ex = null;
        try {
            Reflex.reflect("Nonexistent");
        } catch (AssertionError e) {
            ex = e;
        }
        
        assertNotNull(ex);
        assertEquals("Luokkaa `Nonexistent` ei löytynyt.", ex.getMessage());
        
        try {
            Reflex.reflect(TestSubject.class).staticMethod("getX").returning(int.class).takingNoParams().requireExists();
        } catch (AssertionError e) {
            ex = e;
        }
        
        assertNotNull(ex);
        assertEquals("Luokan TestSubject metodin int getX() pitäisi olla static.", ex.getMessage());
    }
    
}
