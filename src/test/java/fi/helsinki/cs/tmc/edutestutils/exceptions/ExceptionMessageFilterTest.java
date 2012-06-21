package fi.helsinki.cs.tmc.edutestutils.exceptions;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExceptionMessageFilterTest {
    private ExceptionMessageFilter filter;
    
    @Before
    public void setUp() {
        filter = new ExceptionMessageFilter();
        filter.addFunction(new ExceptionMessageFunction() {
            @Override
            public String getNewMessage(Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                    return "it's illegal";
                } else {
                    return null;
                }
            }
        });
    }
    
    @Test
    public void testSimpleException() {
        assertEquals("it's illegal", filter.filter(new IllegalArgumentException("xoox")).getMessage());
    }
    
    @Test
    public void testEmptyMessage() {
        assertEquals("it's illegal", filter.filter(new IllegalArgumentException()).getMessage());
        assertEquals(null, filter.filter(new IllegalStateException()).getMessage());
    }
    
    @Test
    public void testCauses() {
        Throwable in1 = new IllegalArgumentException();
        Throwable in2 = new IllegalStateException("foo", in1);
        Throwable in3 = new IllegalStateException(null, in2);
        Throwable in4 = new IllegalArgumentException("bar", in3);
        
        Throwable out4 = filter.filter(in4);
        Throwable out3 = out4.getCause();
        Throwable out2 = out3.getCause();
        Throwable out1 = out2.getCause();
        
        assertEquals("it's illegal", out4.getMessage());
        assertEquals("foo", out2.getMessage());
        assertNull(out3.getMessage());
        assertEquals("it's illegal", out4.getMessage());
    }
    
    @Test
    public void testPreservesStackTraces() {
        Throwable in1 = new IllegalArgumentException();
        Throwable in2 = new IllegalStateException(null, in1);
        
        Throwable out2 = filter.filter(in2);
        Throwable out1 = out2.getCause();
        
        assertArrayEquals(in1.getStackTrace(), out1.getStackTrace());
        assertArrayEquals(in2.getStackTrace(), out2.getStackTrace());
    }
    
    @Test
    public void testChainingCauseMessages() {
        Throwable in1 = new IllegalArgumentException("foo");
        Throwable in2 = new IllegalStateException("oh no", in1);
        
        filter.setChainingCauseMessages(true);
        Throwable out2 = filter.filter(in2);
        Throwable out1 = out2.getCause();
        
        assertEquals("oh no: it's illegal", out2.getMessage());
        assertEquals("it's illegal", out1.getMessage());
    }
}
