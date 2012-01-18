package fi.helsinki.cs.tmc.edutestutils.timing;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Measures CPU time used by a thread, in seconds, since the object's creation.
 * 
 * <p>
 * The measurement is thread-specific.
 * 
 * <p>
 * <b>NOTE:</b> the accuracy of timers varies over platforms and
 * some platforms may not even support measuring CPU time.
 * For instance, a typical Linux installation seems to give CPU time
 * measurements with an accuracy of 10 milliseconds even though
 * wall clock measurements are much more accurate.
 * Ensure your time limits are lax enough to work on all major platforms.
 */
public class CpuStopwatch {
    public static enum Mode {
        /**
         * Measures time spent in userspace code, excluding time in OS calls.
         */
        USER,
        /**
         * Measures time spent in OS calls, excluding userspace code.
         */
        SYSTEM,
        /**
         * Measures time spent in both userspace code and in OS calls.
         */
        BOTH
    }
    
    private final Mode mode;
    private final long threadId;
    
    private ThreadMXBean bean;
    private long startTime;
    
    /**
     * Creates a CPU stopwatch in the given mode for watching the current thread.
     * 
     * @throws UnsupportedOperationException if the JVM doesn't support CPU stopwatches.
     */
    public CpuStopwatch(Mode mode) throws UnsupportedOperationException {
        this(mode, Thread.currentThread().getId());
    }
    
    /**
     * Creates a CPU stopwatch in the given mode for watching the given thread.
     * 
     * @throws UnsupportedOperationException if the JVM doesn't support CPU stopwatches.
     */
    public CpuStopwatch(Mode mode, long threadId) throws UnsupportedOperationException {
        if (!isSupported()) {
            throw new UnsupportedOperationException("Thread CPU time measurement not supported by this JVM");
        }
        this.mode = mode;
        this.threadId = threadId;
        
        this.bean = ManagementFactory.getThreadMXBean();
        this.bean.setThreadCpuTimeEnabled(true);
        this.startTime = getTime();
    }
    
    private long getTime() {
        switch (mode) {
            case USER: return bean.getThreadUserTime(threadId);
            case SYSTEM: return bean.getThreadCpuTime(threadId) - bean.getThreadUserTime(threadId);
            case BOTH: return bean.getThreadCpuTime(threadId);
            default:
                throw new IllegalStateException("Invalid mode");
        }
    }
    
    /**
     * Tells whether the JVM supports measuring CPU time. Most JVMs do.
     */
    public static boolean isSupported() {
        return ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported();
    }
    
    /**
     * Returns the time elapsed, in seconds, since the creation of the object or a call to {@link #restart()}.
     */
    public double getElapsedTime() {
        return (getTime() - startTime) / 1000000000.0;
    }
    
    /**
     * Resets the stopwatch to 0 as if it had just been created.
     */
    public void restart() {
        startTime = getTime();
    }

    /**
     * Returns a debug string like {@code "Elapsed [user/system] CPU time: 1.23"}.
     */
    @Override
    public String toString() {
        if (mode == Mode.USER) {
            return "Elapsed user CPU time: " + getElapsedTime();
        } else if (mode == Mode.SYSTEM) {
            return "Elapsed system CPU time: " + getElapsedTime();
        } else {
            return "Elapsed CPU time: " + getElapsedTime();
        }
    }
}
