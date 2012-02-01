package fi.helsinki.cs.tmc.edutestutils;

import java.util.Scanner;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.NoSuchElementException;

public class MockInOutTest {

    @Test public void test() {
        Object out = System.out;
        Object in = System.in;
        MockInOut mio = new MockInOut("1\nXYZ\n");
        assertFalse(System.out==out);
        assertFalse(System.in==in);
        System.out.println("hello");
        System.out.println("moi");
        
        assertEquals("hello\nmoi\n",mio.getOutput());

        Scanner s = new Scanner(System.in);

        assertEquals("1",s.nextLine());
        assertEquals("XYZ",s.nextLine());
        try {
            s.nextLine();
            fail("Reading past the end of input succeeded!");
        } catch (NoSuchElementException e) {
            // yay!
        }

        mio.close();

        assertTrue(System.out==out);
        assertTrue(System.in==in);
    }

}