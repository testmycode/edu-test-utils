package fi.helsinki.cs.tmc.edutestutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * &nbsp;&nbsp;&#64;Rule<br>
 * &nbsp;&nbsp;public MockStdio io = new MockStdio();<br>
 * &nbsp;&nbsp;<br>
 * &nbsp;&nbsp;// ...<br>
 * }
 * </code>
 * 
 * <p>This class automatically converts line endings in stdout and stderr to
 * unix format (only <tt>\n</tt>) for your convenience.
 * 
 * <p>
 * NOTE: It's common for student code to have a {@link Scanner} object with
 * a reference to {@link System#in} in a <b>static</b> variable.
 * If the static variable is
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19075">initialized</a>
 * before this rule gets a chance to change {@link System#in},
 * then the scanner will read from the actual input stream.
 * It may be necessary to use
 * {@link ReflectionUtils#reloadClass(java.lang.String)} to get a new instance
 * of the student code in order to make it point to the changed {@link System#in}.
 */
public class MockStdio implements TestRule {
    
    private static class MockInputStream extends InputStream {
        public int pos = 0;
        public byte[] source = new byte[0];
        
        @Override
        public int read() throws IOException {
            if (pos < source.length) {
                return source[pos++];
            } else {
                return -1;
            }
        }
    }
    
    private Charset charset = Charset.defaultCharset();
    
    private InputStream origIn;
    private PrintStream origOut;
    private PrintStream origErr;
    
    private MockInputStream mockIn;
    private ByteArrayOutputStream outBuf;
    private ByteArrayOutputStream errBuf;
    
    @Override
    public Statement apply(final Statement stmt, Description d) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                origIn = System.in;
                origOut = System.out;
                origErr = System.err;
                
                mockIn = new MockInputStream();
                outBuf = new ByteArrayOutputStream();
                errBuf = new ByteArrayOutputStream();
                
                System.setIn(mockIn);
                System.setOut(new PrintStream(outBuf, true, charset.name()));
                System.setErr(new PrintStream(errBuf, true, charset.name()));
                
                stmt.evaluate();
                
                mockIn = null;
                outBuf = null;
                errBuf = null;
                
                System.setIn(origIn);
                System.setOut(origOut);
                System.setErr(origErr);
            }
        };
    }
    
    /**
     * Sets the what {@link System#in} receives during this test.
     * @param str 
     */
    public void setSysIn(String str) {
        mockIn.source = str.getBytes(charset);
        mockIn.pos = 0;
    }
    
    /**
     * Returns what was printed to {@link System#out} during this test.
     */
    public String getSysOut() {
        try {
            return outBuf.toString(charset.name()).replace("\r\n", "\n");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns what was printed to {@link System#err} during this test.
     */
    public String getSysErr() {
        try {
            return errBuf.toString(charset.name()).replace("\r\n", "\n");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
