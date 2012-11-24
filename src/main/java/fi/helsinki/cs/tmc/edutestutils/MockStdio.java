package fi.helsinki.cs.tmc.edutestutils;

import fi.helsinki.cs.tmc.edutestutils.utils.SwitchableInputStream;
import fi.helsinki.cs.tmc.edutestutils.utils.SwitchableOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Scanner;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule that captures output from {@code System.out} and {@code System.err} and feeds input to {@code System.in}.
 * 
 * <p>
 * Redirects {@link System#in}, {@link System#out} and {@link System#err}
 * to buffers before each test and resets them after the test.
 * Use it like this
 * 
 * <p>
 * <code>
 * import org.junit.Rule;<br>
 * <br>
 * public class MyTest {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&#64;Rule<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;public MockStdio io = new MockStdio();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;// ...<br>
 * }
 * </code>
 * 
 * <p>This class automatically converts line endings in stdout and stderr to
 * unix format (only <tt>\n</tt>).
 * 
 * <h2>Important notes about initialization order</h2>
 * 
 * <p>
 * It's common for student code to have a {@link Scanner} object with
 * a reference to {@link System#in} in a <b>static</b> variable.
 * If the static variable is
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19075">initialized</a>
 * before this rule gets a chance to change {@link System#in},
 * then the scanner will read from the original input stream
 * instead of the mock.
 * 
 * <p>
 * If MockStdio used by <b>all</b> test classes
 * (critically, by the first test class to be run), then this is not a problem,
 * since MockStdio gets a chance to (permanently) replace {@link System#in}
 * before any student classes get initialized. If MockStdio is not used in all
 * test classes (i.e. it's possible a test class without MockStdio gets run
 * before one that has MockStdio) then it may be necessary to use
 * {@link ReflectionUtils#newInstanceOfClass(java.lang.String)}
 * to get a new instance of the student code in order to make it point to
 * the changed {@link System#in}.
 * 
 * <p>
 * MockStdio has been known to cause problems on some JVMs
 * when used with PowerMock. The simpler {@link MockInOut} may be helpful in
 * those cases.
 * 
 * @see MockInOut
 */
public class MockStdio implements TestRule {

    private static boolean initialized = false;
    
    private static final Charset charset;
    static {
        // Our source and tests files are UTF-8, so we assert against UTF-8 strings
        // and don't want MockInOut to convert to the native charset.
        if (Charset.availableCharsets().containsKey("UTF-8")) {
            charset = Charset.forName("UTF-8");
        } else {
            charset = Charset.defaultCharset();
        }
    }
    
    private static final InputStream realIn = System.in;
    private static final OutputStream realOut = System.out;
    private static final OutputStream realErr = System.err;
    
    private static final SwitchableInputStream switchIn = new SwitchableInputStream(realIn);
    private static final SwitchableOutputStream switchOut = new SwitchableOutputStream(realOut);
    private static final SwitchableOutputStream switchErr = new SwitchableOutputStream(realErr);
    
    private InputStream mockIn;
    private ByteArrayOutputStream mockOut;
    private ByteArrayOutputStream mockErr;
    private boolean enabled;
    
    @Override
    public Statement apply(final Statement stmnt, Description d) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    if (!enabled) {
                        enable();
                    }
                    stmnt.evaluate();
                } finally {
                    if (enabled) {
                        disable();
                    }
                }
            }
        };
    }
    
    private void initialize() {
        resetMockIn();
        resetMockOutAndErr();
        
        System.setIn(switchIn);
        try {
            System.setOut(new PrintStream(switchOut, true, charset.name()));
            System.setErr(new PrintStream(switchErr, true, charset.name()));
        } catch (UnsupportedEncodingException ex) {
            throw new Error(ex);
        }
        
        initialized = true;
    }
    
    private void resetMockIn() {
        mockIn = new ByteArrayInputStream(new byte[0]);
    }
    
    private void resetMockOutAndErr() {
        mockOut = new ByteArrayOutputStream();
        mockErr = new ByteArrayOutputStream();
    }
    
    /**
     * Sets what {@link System#in} receives during this test.
     */
    public void setSysIn(String str) {
        mockIn = new ByteArrayInputStream(str.getBytes(charset));
        if (enabled) {
            switchIn.setUnderlying(mockIn);
        }
    }
    
    /**
     * Returns what was printed to {@link System#out} during this test.
     */
    public String getSysOut() {
        try {
            return mockOut.toString(charset.name()).replace("\r\n", "\n");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns what was printed to {@link System#err} during this test.
     */
    public String getSysErr() {
        try {
            return mockErr.toString(charset.name()).replace("\r\n", "\n");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Redirects I/O to/from buffers.
     * 
     * <p>
     * If your use MockStdio as a JUnit rule, there is no need to call
     * this directly.
     */
    public void enable() {
        if (!initialized) {
            initialize();
        }
        
        resetMockOutAndErr();
        
        switchIn.setUnderlying(mockIn);
        switchOut.setUnderlying(mockOut);
        switchErr.setUnderlying(mockErr);
        
        enabled = true;
    }
    
    /**
     * Tells whether I/O is directed to/from the mock buffers.
     * 
     * <p>
     * Should be true in all tests with this JUnit rule.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Redirects I/O to/from the original streams.
     * 
     * <p>
     * If your use MockStdio as a JUnit rule, there is no need to call
     * this directly.
     */
    public void disable() {
        enabled = false;
        
        switchIn.setUnderlying(realIn);
        switchOut.setUnderlying(realOut);
        switchErr.setUnderlying(realErr);
        
        resetMockIn();
    }
    
}
