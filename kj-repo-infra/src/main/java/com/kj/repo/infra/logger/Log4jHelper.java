package com.kj.repo.infra.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class Log4jHelper {

    public static Logger getLogger() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        return getLogger(elements[elements.length - 1].getClassName());
    }

    public static void initialize(String location) {
        Configurator.initialize(LogManager.class.getName(), Log4jHelper.class.getClassLoader(), location);
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

}
