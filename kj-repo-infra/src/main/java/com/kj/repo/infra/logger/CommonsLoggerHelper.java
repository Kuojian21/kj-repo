package com.kj.repo.infra.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class CommonsLoggerHelper {
    public static Log getLogger() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        return getLogger(elements[elements.length - 2].getClassName());
    }

    public static Log getLogger(Class<?> clazz) {
        return LogFactory.getLog(clazz);
    }

    public static Log getLogger(String name) {
        return LogFactory.getLog(name);
    }

}
