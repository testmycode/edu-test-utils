package fi.helsinki.cs.tmc.edutestutils.utils;

import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * Delegates to another output stream that can be switched at any time.
 */
public class SwitchableOutputStream extends FilterOutputStream {

    public SwitchableOutputStream(OutputStream out) {
        super(out);
    }

    public OutputStream getUnderlying() {
        return out;
    }
    
    public void setUnderlying(OutputStream out) {
        this.out = out;
    }
    
}
