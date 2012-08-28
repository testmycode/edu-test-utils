package fi.helsinki.cs.tmc.edutestutils.classloaders;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A class loader whose findClass() closely resembles the default.
 */
public abstract class AbstractClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InputStream classIn = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (classIn == null) {
            throw new ClassNotFoundException(name);
        }
        classIn = new BufferedInputStream(classIn);

        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            int data = classIn.read();
            while (data != -1) {
                buf.write(data);
                data = classIn.read();
            }

            byte[] classDef = buf.toByteArray();
            return defineClass(name, classDef, 0, classDef.length);
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                classIn.close();
            } catch (IOException e) {
            }
        }
    }
}
