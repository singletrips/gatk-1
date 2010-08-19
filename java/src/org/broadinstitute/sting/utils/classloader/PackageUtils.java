/*
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.utils.classloader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.broadinstitute.sting.utils.StingException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * PackageUtils contains some useful methods for package introspection.
 */
public class PackageUtils {

    /**
     * A reference into our introspection utility.
     */
    private static Reflections reflections = null;

    static {
        // turn off logging in the reflections library - they talk too much (to the wrong logger factory as well, logback)
        Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Reflections.class);
        logger.setLevel(Level.OFF);

        // Initialize general-purpose source tree reflector.
        reflections = new Reflections( new ConfigurationBuilder()
            .setUrls(getClassPathURLs())
            .setScanners(new SubTypesScanner()));
    }

    /**
     * Private constructor.  No instantiating this class!
     */
    private PackageUtils() {}
    {
    }

    /**
     * Return the classes that implement the specified interface.
     *
     * @param iface  the interface which returned classes should implement.
     * @return       the list of classes that implement the interface.
     */
    public static <T> List<Class<? extends T>> getClassesImplementingInterface(Class<T> iface) {
        // Load all classes implementing the given interface, then filter out any class that isn't concrete.
        Set<Class<? extends T>> allTypes = reflections.getSubTypesOf(iface);
        List<Class<? extends T>> concreteTypes = new ArrayList<Class<? extends T>>();
        for( Class<? extends T> type: allTypes ) {
            if( JVMUtils.isConcrete(type) )
                concreteTypes.add(type);
        }

        return concreteTypes;
    }

    public static <T> List<T> getInstancesOfClassesImplementingInterface(Class<T> iface) {
        List<Class<? extends T>> classes = PackageUtils.getClassesImplementingInterface(iface);
        List<T> instances = new ArrayList<T>();
        for ( Class<? extends T> c : classes )
            instances.add(getSimpleInstance(c));
        return instances;
    }

    public static <T> T getSimpleInstance(Class<T> c) {
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            throw new StingException(String.format("Cannot instantiate class '%s': must be concrete class", c.getSimpleName()));
        } catch (IllegalAccessException e) {
            throw new StingException(String.format("Cannot instantiate class '%s': must have no-arg constructor", c.getSimpleName()));
        }
    }

    /**
     * Return the interface classes that extend the specified interface.
     *
     * @param iface  the interface which returned classes should extend.
     * @return       the list of interface classes that implement the interface.
     */
    public static <T> List<Class<? extends T>> getInterfacesExtendingInterface(Class<T> iface) {
        // Load all classes extending the given interface, then filter out any class that is concrete.
        Set<Class<? extends T>> allTypes = reflections.getSubTypesOf(iface);
        List<Class<? extends T>> nonConcreteTypes = new ArrayList<Class<? extends T>>();
        for( Class<? extends T> type: allTypes ) {
            if( !JVMUtils.isConcrete(type) )
                nonConcreteTypes.add(type);
        }

        return nonConcreteTypes;
    }

    public static Set<URL> getClassPathURLs() {
        return ClasspathHelper.getUrlsForCurrentClasspath();
    }
}
