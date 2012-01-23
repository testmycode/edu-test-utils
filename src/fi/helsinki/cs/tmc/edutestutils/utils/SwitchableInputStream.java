package fi.helsinki.cs.tmc.edutestutils.utils;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Delegates to another input stream that can be switched at any time.
 */
public class SwitchableInputStream extends FilterInputStream {

    public SwitchableInputStream(InputStream out) {
        super(out);
    }

    public InputStream getUnderlying() {
        return in;
    }
    
    public void setUnderlying(InputStream in) {
        this.in = in;
    }
    
}
