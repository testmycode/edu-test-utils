package fi.helsinki.cs.tmc.edutestutils.timing;

/**
 * Thrown by {@link CpuTimeLimit} when a test takes too long.
 */
public class TimeLimitException extends RuntimeException {
    public TimeLimitException(String message) {
        super(message);
    }
}
