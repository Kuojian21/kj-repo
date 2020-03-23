package com.kj.repo.infra.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.helper.JavaHelper;

/**
 * @author kj
 * Created on 2020-03-21
 */
public class LoggerHelper {

    public static Logger getLogger() {
        return getLogger(JavaHelper.stack(2).getClassName());
    }

    public static Logger asyncLogger() {
        return getLogger(JavaHelper.stack(2).getClassName());
    }

    public static Logger syncLogger() {
        return getLogger("sync." + JavaHelper.stack(2).getClassName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

}
