package fi.helsinki.cs.tmc.edutestutils.exceptions;

/**
 * See {@link ExceptionMessageFilter}.
 */
public interface ExceptionMessageFunction {
    /**
     * Returns a new message for the given exception, or null to leave unchanged.
     * 
     * <p>
     * Exception causes are processed depth-first.
     */
    public String getNewMessage(Throwable ex);
}
