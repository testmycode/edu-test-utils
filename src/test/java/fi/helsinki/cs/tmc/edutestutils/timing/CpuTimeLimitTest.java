package fi.helsinki.cs.tmc.edutestutils.timing;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CpuTimeLimitTest {
    public CpuTimeLimit timeLimit = new CpuTimeLimit();
    
    @Before
    public void checkThatCpuStopwatchIsSupported() {
        assertTrue(CpuStopwatch.isSupported());
    }
    
    @Test(expected=TimeLimitException.class)
    public void imposesTimeLimitGivenInTestMethod() throws Throwable {
        runWithTimeLimit(new Runnable() {
            @Override
            public void run() {
                timeLimit.set(0.5);
                while (true) {
                }
            }
        });
    }
    
    @Test
    public void imposesNoTimeLimitByDefault() throws Throwable {
        runWithTimeLimit(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < start + 1500) {
                }
            }
        });
    }
    
    @Test(expected=TimeLimitException.class)
    public void mayBeGivenADefaultTimeLimit() throws Throwable {
        timeLimit = new CpuTimeLimit(0.1);
        runWithTimeLimit(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < start + 3000) {
                }
            }
        });
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void passesThroughExceptionsInTests() throws Throwable {
        runWithTimeLimit(new Runnable() {
            @Override
            public void run() {
                throw new IllegalArgumentException();
            }
        });
    }
    
    @Test
    public void measuresCpuTime() throws Throwable {
        runWithTimeLimit(new Runnable() {
            @Override
            public void run() {
                timeLimit.set(2.0);
                try {
                    // Thread.sleep should not use CPU time but should use real time
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                }
            }
        });
    }
    
    private void runWithTimeLimit(final Runnable runnable) throws Throwable {
        timeLimit.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                runnable.run();
            }
        }, Description.EMPTY).evaluate();
    }
}
