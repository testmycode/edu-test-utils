package fi.helsinki.cs.tmc.edutestutils.exceptions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static org.junit.Assert.*;

public class ExceptionMessageFilterAsRuleTest {
    @Rule
    public ExceptionMessageFilter filter = new ExceptionMessageFilter();
    
    @Rule
    public TestRule catchingRule = new TestRule() {
        @Override
        public Statement apply(final Statement stmnt, Description d) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        stmnt.evaluate();
                    } catch (IllegalArgumentException e) {
                        assertEquals("newmsg", e.getMessage());
                        return;
                    }
                    fail("Didn't catch expected exception");
                }
            };
        }
    };
    
    @Test
    public void testIt() {
        filter.addFunction(new ExceptionMessageFunction() {
            @Override
            public String getNewMessage(Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                    return "newmsg";
                } else {
                    return null;
                }
            }
        });
        
        throw new IllegalArgumentException("oldmsg");
    }
}
