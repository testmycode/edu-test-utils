package fi.helsinki.cs.tmc.edutestutils;

import org.junit.Test;
import static fi.helsinki.cs.tmc.edutestutils.EduAssert.*;

public class BetterAssertTest {
    
    @Test
    public void testAssertMatches_FullMatchIsOk() {
        assertMatches("a|b", "a");
        assertMatches("darn", "(a|b)*", "aabbbab");
    }
    
    @Test(expected=AssertionError.class)
    public void testAssertMatches_PartialMatchesAreNotAccepted() {
        assertMatches("darn", "a|b", "bbb");
    }
    
    @Test
    public void testAssertMatches_MatchFailureWithDefaultMessage() {
        AssertionError ex = null;
        try {
            assertMatches("a|b", "c");
        } catch (AssertionError e) {
            ex = e;
        }
        assertNotNull(ex);
        assertEquals("\"c\" does not have the required form.", ex.getMessage());
    }
    
    @Test
    public void testAssertMatches_MatchFailureWithCustomMessage() {
        AssertionError ex = null;
        try {
            assertMatches("oh shoot", "a|b", "c");
        } catch (AssertionError e) {
            ex = e;
        }
        assertNotNull(ex);
        assertEquals("oh shoot", ex.getMessage());
    }
    
    @Test(expected=AssertionError.class)
    public void testAssertContainsNumber_ThrowsOnError() {
        assertContainsNumber(1.23, "1.24");
    }
    
    @Test
    public void testContainsNumber_PositiveCases() {
        assertContainsNumber(7, "7");
        assertContainsNumber(7.8, "7.8");
        assertContainsNumber(7.8, "7,8");
        assertContainsNumber(7.8, "07,80");
        assertContainsNumber(-7.8, "-07,80");
        assertContainsNumber(-7.8, "foo-07,80bar");
        
        assertContainsNumber(1.23, "The interesting result is 001,2300.");
        assertContainsNumber(200, "\"200\" it is!");
        
        assertContainsNumber(100, "100, 200, 300!");
        assertContainsNumber(200, "100, 200, 300!"); // (but 100,200,300 without spaces is problematic)
        assertContainsNumber(300, "100, 200, 300!");
        
        assertContainsNumber(20.2, "!!20,20!!");
        assertContainsNumber(20.2, "!!+20,20!!");
        assertContainsNumber(-20.2, "!!-20,20!!");
        
        assertContainsNumber(1, "abc1.00000001def");
    }
    
    @Test
    public void testContainsNumber_NegativeCases() {
        assertDoesNotContainNumber(8, "7");
        assertDoesNotContainNumber(1000, "10000");
        assertDoesNotContainNumber(1000, "100");
        assertDoesNotContainNumber(1000, "1 000");
        assertDoesNotContainNumber(-1000, "- 1000");
    }
    
    private void assertDoesNotContainNumber(double number, String actual) {
        if (containsNumber(number, actual)) {
            fail();
        }
    }
}
