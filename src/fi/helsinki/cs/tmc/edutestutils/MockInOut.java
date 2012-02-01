package fi.helsinki.cs.tmc.edutestutils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * A simple class for capturing {@code System.out} and injecting {@code System.in}.
 *
 * <p>
 * Usage:</p>
 *
 * <code>
 * public void myTest() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;MockInOut mio = new MockInOut("input goes here");<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;// Call some code<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;String out = mio.getOutput();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;mio.close();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;// Check that the output is correct<br>
 * }<br>
 * </code>
 */
public class MockInOut {

    private PrintStream orig;
    private InputStream irig;
    private ByteArrayOutputStream os;
    private ByteArrayInputStream is;

    public MockInOut(String input) {
        orig = System.out;
        irig = System.in;

        os = new ByteArrayOutputStream();
        System.setOut(new PrintStream(os));

        is = new ByteArrayInputStream(input.getBytes());
        System.setIn(is);
    }

    /** You can use this if you want to check how much of the input was read. */
    public ByteArrayInputStream getInputStream() {
        return is;
    }        

    /** Returns everything written to System.out since this {@code MockInOut}
     * was constructed. Can't be called on a closed {@code MockInOut} */
    public String getOutput() {
        if (os != null)
            return os.toString();
        else
            throw new Error("getOutput on closed MockInOut!");
    }

    /** Restores System.in and System.out */
    public void close() {
        os = null;
        is = null;
        System.setOut(orig);
        System.setIn(irig);
    }
}

