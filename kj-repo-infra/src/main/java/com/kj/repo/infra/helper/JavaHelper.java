package com.kj.repo.infra.helper;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author kj
 * Created on 2020-03-18
 */
public class JavaHelper {

    private static final Set<ClassLoader> loaders;

    static {
        Set<ClassLoader> tLoaders = Sets.newHashSet(ClassLoader.getSystemClassLoader());
        loaders(JavaHelper.class.getClassLoader(), tLoaders);
        loaders(Thread.currentThread().getContextClassLoader(), tLoaders);
        loaders = Collections.unmodifiableSet(tLoaders);
    }

    private static Set<ClassLoader> loaders(ClassLoader classLoader, Set<ClassLoader> sets) {
        if (classLoader != null && !sets.contains(classLoader)) {
            sets.add(classLoader);
            loaders(classLoader.getParent(), sets);
        }
        return sets;
    }

    /**
     * org.apache.logging.log4j.util.StackLocator#getCallerClass(int)
     * sun.reflect.Reflection#getCallerClass()
     */
    public static StackTraceElement stack(int depth) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (depth <= 0) {
            depth = 1;
        } else if (depth > elements.length) {
            depth = elements.length;
        }
        return elements[elements.length - depth];
    }

    public static Set<ClassLoader> loaders() {
        Set<ClassLoader> rtn = Sets.newHashSet(loaders);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            loaders(loader, rtn);
        }
        return rtn;
    }

    public static URL location(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    public static List<URL> resources(String resource) throws IOException {
        List<URL> rtn = Lists.newArrayList();
        for (ClassLoader classLoader : loaders()) {
            Enumeration<URL> t = classLoader.getResources(resource);
            while (t.hasMoreElements()) {
                rtn.add(t.nextElement());
            }
        }
        return rtn;
    }

    public static <T> List<T> services(Class<T> clazz) {
        List<T> rtn = Lists.newArrayList();
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        for (T t : loader) {
            rtn.add(t);
        }
        return rtn;
    }

}
