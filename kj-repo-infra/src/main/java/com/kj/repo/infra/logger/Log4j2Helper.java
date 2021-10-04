package com.kj.repo.infra.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.kj.repo.infra.utils.JavaUtil;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class Log4j2Helper {

    public static void initialize(String location) {
        Configurator.initialize(LogManager.class.getName(), Log4j2Helper.class.getClassLoader(), location);
    }

    public static Logger getLogger() {
        return getLogger(JavaUtil.stack(2).getClassName());
    }

    public static Logger asyncLogger() {
        return getLogger(JavaUtil.stack(2).getClassName());
    }

    public static Logger syncLogger() {
        return getLogger("sync." + JavaUtil.stack(2).getClassName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

}
