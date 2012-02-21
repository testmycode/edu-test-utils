package fi.helsinki.cs.tmc.edutestutils.exceptions;

import java.util.ArrayList;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Changes the message on exceptions (and/or their causes).
 * 
 * <p>
 * Usage:
 * <pre>
 * {@code
 * ExceptionMessageFilter filter = new ExceptionMessageFilter();
 * 
 * filter.addFunction(new ExceptionMessageFunction() {
 *     public String getNewMessage(Throwable ex) {
 *         if (ex instanceof AssertionError && ex.getMessage().equals("timeout")) {
 *             return "Your program was too slow.";
 *         }
 *         return null;
 *     }
 * });
 * }
 * </pre>
 * 
 * <p>
 * When used as a JUnit rule, it filters exceptions thrown by tests.
 */
public class ExceptionMessageFilter implements TestRule {
    private boolean chainingCauseMessages;
    private List<ExceptionMessageFunction> functions;

    public ExceptionMessageFilter() {
        chainingCauseMessages = false;
        functions = new ArrayList<ExceptionMessageFunction>();
    }
    
    /**
     * Adds a filtering callback.
     * 
     * <p>
     * If there are multiple callbacks then they are invoked in order
     * until one returns a non-null value.
     */
    public void addFunction(ExceptionMessageFunction func) {
        functions.add(func);
    }
    
    /**
     * Sets whether to make the toplevel exception include the messages from all its causes.
     * 
     * <p>
     * The message would have the form "toplevelmsg1: cause1msg: cause2msg: ...".
     * 
     * <p>
     * Defaults to false.
     */
    public void setChainingCauseMessages(boolean enable) {
        this.chainingCauseMessages = enable;
    }
    
    /**
     * Creates a new exception (chain) whose message(s) may be changed.
     * 
     * <p>
     * Stack traces are preserved.
     */
    public Throwable filter(Throwable ex) {
        Throwable cause;
        if (ex.getCause() != null) {
            cause = filter(ex.getCause());
        } else {
            cause = null;
        }
        
        String message = newMessageFor(ex);
        
        if (chainingCauseMessages && message != null && cause != null && cause.getMessage() != null) {
            message = message + ": " + cause.getMessage();
        }
        
        try {
            Throwable newEx = ex.getClass().getConstructor(String.class).newInstance(message);
            newEx.initCause(cause);
            newEx.setStackTrace(ex.getStackTrace());
            return newEx;
        } catch (Exception e) {
            // Fail silently on the various possible reflection errors
            return ex;
        }
    }
    
    private String newMessageFor(Throwable ex) {
        for (ExceptionMessageFunction func : functions) {
            String msg = func.getNewMessage(ex);
            if (msg != null) {
                return msg;
            }
        }
        return ex.getMessage();
    }

    /**
     * Implements a JUnit rule that filters exceptions thrown by tests.
     */
    @Override
    public Statement apply(final Statement stmnt, Description d) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    stmnt.evaluate();
                } catch (Throwable t) {
                    throw filter(t);
                }
            }
        };
    }
}
