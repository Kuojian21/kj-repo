package com.kj.repo.infra.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.kj.repo.infra.helper.JavaHelper;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class Log4jHelper {

    public static void initialize(String location) {
        Configurator.initialize(LogManager.class.getName(), Log4jHelper.class.getClassLoader(), location);
    }

    public static String getName() {
        return JavaHelper.stack(2).getClassName();
    }

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
        return LogManager.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

}
