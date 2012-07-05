package fi.helsinki.cs.tmc.edutestutils;

import fi.helsinki.cs.tmc.edutestutils.Reflex.MethodRef1;
import fi.helsinki.cs.tmc.edutestutils.Reflex.MethodRef0;
import fi.helsinki.cs.tmc.edutestutils.Reflex.ClassRef;
import java.util.Locale;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReflexTest {
    
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
    public void localizedErrorMessages() throws Throwable {
        ReflectionUtils.setMsgLocale(new Locale("fi"));
        
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
