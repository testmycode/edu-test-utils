package fi.helsinki.cs.tmc.edutestutils.timing;

import java.text.DecimalFormat;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Imposes test method specific CPU limits in seconds.
 * 
 * <p>
 * Usage:
 * 
 * <p>
 * <code>
 * import org.junit.Rule;<br>
 * <br>
 * public class MyTest {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&#64;Rule<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;public TimeLimit timeLimit = new TimeLimit();<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;@Test<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;public void testSomething() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;timeLimit.set(3.5);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;// test stuff<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * }
 * </code>
 * 
 * <p>
 * By default, user CPU time is measured.
 * 
 * <p>
 * The rule does nothing unless a default timeout is given as
 * a constructor parameter. A test method may call {@link #set(double)}
 * to set a test method specific time limit. If the timeout expires,
 * the test will terminate with a {@link TimeLimitException}.
 * 
 * <p>
 * JUnit also has a {@link Timeout} rule which measures real time and
 * sets the same timeout for all test methods in a test class.
 * There is also the test method specific <code>@Test(timeout=123)</code>
 * option, but it too measures real time.
 * 
* <p>
 * <b>NOTE:</b> the accuracy of timers varies over platforms and
 * some platforms may not even support measuring CPU time.
 * For instance, a typical Linux installation seems to give CPU time
 * measurements with an accuracy of 10 milliseconds even though
 * wall clock measurements are much more accurate.
 * Ensure your time limits are lax enough to work on all major platforms.
 */
public class CpuTimeLimit implements TestRule {
    private static final long TIMEOUT_CHECK_INTERVAL = 500; // How often to check whether we've timed out.
    private static final CpuStopwatch.Mode DEFAULT_STOPWATCH_MODE = CpuStopwatch.Mode.USER;
    
    private final double defaultTimeLimit;
    private final CpuStopwatch.Mode stopwatchMode;
    
    private volatile double timeLimit = Double.MAX_VALUE;
    
    /**
     * Constructs a time limit rule with no default time limit and a user CPU time stopwatch.
     */
    public CpuTimeLimit() {
        this(Double.MAX_VALUE, DEFAULT_STOPWATCH_MODE);
    }

    /**
     * Constructs a time limit rule with a default time limit and a user CPU time stopwatch.
     */
    public CpuTimeLimit(double defaultTimeLimit) {
        this(defaultTimeLimit, DEFAULT_STOPWATCH_MODE);
    }
    
    /**
     * Constructs a time limit rule with no default time limit and the given stopwatch mode.
     */
    public CpuTimeLimit(CpuStopwatch.Mode stopwatchMode) {
        this(Double.MAX_VALUE, stopwatchMode);
    }
    
    /**
     * Constructs a time limit rule with a default time limit and the given stopwatch type.
     */
    public CpuTimeLimit(double defaultTimeLimit, CpuStopwatch.Mode stopwatchMode) {
        this.defaultTimeLimit = defaultTimeLimit;
        this.stopwatchMode = stopwatchMode;
    }
    
    /**
     * Call this from a test method to set a time limit for that method.
     * 
     * <p>
     * The time limit will only apply to the current test method.
     * The default time limit (if any) will be applied for any
     * subsequent test methods.
     */
    public synchronized void set(double limit) {
        this.timeLimit = limit;
    }
    
    /**
     * Implements {@link TestRule}.
     */
    @Override
    public Statement apply(final Statement statement, Description d) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                timeLimit = defaultTimeLimit;
                
                TestThread thread = new TestThread(statement);
                thread.start();
                
                CpuStopwatch stopwatch = createStopwatch(thread.getId());
                while (!thread.finished && stopwatch.getElapsedTime() < timeLimit) {
                    thread.join(TIMEOUT_CHECK_INTERVAL);
                }
                
                Throwable exception = null;
                if (thread.finished) {
                    exception = thread.exceptionFromTest;
                } else {
                    exception = new TimeLimitException("Time limit (" + timeLimitString() + ") exceeded");
                    exception.setStackTrace(thread.getStackTrace());
                    thread.interrupt();
                }
                
                if (exception != null) {
                    throw exception;
                }
            }
        };
    }
    
    private String timeLimitString() {
        return new DecimalFormat("#.###s").format(timeLimit);
    }
    
    private CpuStopwatch createStopwatch(long threadId) {
        return new CpuStopwatch(stopwatchMode, threadId);
    }

    
    private static class TestThread extends Thread {
        private final Statement statement;
        
        public boolean finished = false;
        public Throwable exceptionFromTest = null;

        public TestThread(Statement statement) {
            super("TimeLimit.TestThread");
            this.statement = statement;
        }

        @Override
        public void run() {
            try {
                statement.evaluate();
            } catch (InterruptedException e) {
                // Most likely sent by us after a timeout
            } catch (Throwable e) {
                exceptionFromTest = e;
            } finally {
                finished = true;
            }
        }
    }
    
}
