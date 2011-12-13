package fi.helsinki.cs.tmc.edutestutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a test must pass for <tt>tmc-junit-runner</tt> to give the given points.
 * 
 * <p>
 * The value must be a <b>space</b>-separated list of point names.
 * 
 * <p>
 * Annotating a JUnit test method {@code m} with <code>@Points("1 2 3")</code>
 * means that points 1, 2 and 3 may only be awarded if {@code m} passes.
 * If several test methods are annotated with a given point name,
 * then all of those test methods must pass for the point to be awarded.
 * 
 * <p>
 * A <code>@Points</code> annotation applied to a class is applied to all
 * test methods in that class. If a test method has the annotation as well,
 * the points in the class annotation are added to it.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface Points {
    public String value();
}

