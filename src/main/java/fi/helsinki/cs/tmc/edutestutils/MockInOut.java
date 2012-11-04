package fi.helsinki.cs.tmc.edutestutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * A simple class for capturing {@code System.out} and injecting
 * {@code System.in}.
 *
 * <p>Usage:</p>
 *
 * <code>
 * public void myTest() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;MockInOut mio = new MockInOut("input goes here");<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;try {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// Call some code<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;String out = mio.getOutput();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;} finally {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mio.close();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;// Check that the output is correct<br>
 * }<br>
 * </code>
 *
 * This class automatically converts line endings in stdout to unix format (only
 * \n).
 *
 * @see MockStdio
 */
public class MockInOut {

    private PrintStream orig;
    private InputStream irig;
    private ByteArrayOutputStream os;
    private ByteArrayInputStream is;
    private final static Charset charset;
    
    static {
        if (Charset.availableCharsets().containsKey("UTF-8")) {
            charset = Charset.forName("UTF-8");
        } else {
            charset = Charset.defaultCharset();
        }
    }

    public MockInOut(String input) {
        orig = System.out;
        irig = System.in;

        os = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(os, false, charset.name()));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        is = new ByteArrayInputStream(input.getBytes());
        System.setIn(is);
    }

    /**
     * You can use this if you want to check how much of the input was read.
     */
    public ByteArrayInputStream getInputStream() {
        return is;
    }

    /**
     * Returns everything written to System.out since this {@code MockInOut} was
     * constructed. Can't be called on a closed {@code MockInOut}
     */
    public String getOutput() {
        if (os != null) {
            try {
                return os.toString(charset.name()).replace("\r\n", "\n");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new Error("getOutput on closed MockInOut!");
        }
    }

    /**
     * Restores System.in and System.out
     */
    public void close() {
        os = null;
        is = null;
        System.setOut(orig);
        System.setIn(irig);
    }
}
