package fi.helsinki.cs.tmc.edutestutils;

import java.util.Scanner;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class MockStdioTest {
    @Rule
    public MockStdio io = new MockStdio();
    
    @Test
    public void capturesStdout() {
        System.out.println("hello");
        assertEquals("hello\n", io.getSysOut());
        assertTrue(io.getSysErr().isEmpty());
    }
    
    @Test
    public void capturesStderr() {
        System.err.println("hello");
        assertEquals("hello\n", io.getSysErr());
        assertTrue(io.getSysOut().isEmpty());
    }
    
    @Test
    public void allowsSettingStdin() {
        io.setSysIn("hello");
        assertEquals("hello", new Scanner(System.in).nextLine());
    }
}
