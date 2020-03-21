package com.kj.repo.infra.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kj
 * Created on 2020-03-21
 */
public class LoggerHelper {

    public static Logger getLogger() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        return getLogger(elements[elements.length - 1].getClassName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

}
