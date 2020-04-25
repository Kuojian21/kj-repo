package com.kj.repo.infra.logger;

import java.net.URL;

import org.slf4j.Logger;

import com.kj.repo.infra.helper.JavaHelper;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class LogbackHelper {

    private static final LoggerContext loggerContext = new LoggerContext();

    static {
        init();
    }

    public static void init() {
        try {
            loggerContext.reset();
            new ContextInitializer(loggerContext).autoConfig();
        } catch (JoranException e) {
            e.printStackTrace();
        }
    }

    public static void initialize(String location) {
        initialize(Thread.currentThread().getContextClassLoader().getResource(location));
    }

    public static void initialize(URL location) {
        try {
            //            LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton()
            //            .getLoggerFactory();
            loggerContext.reset();
            JoranConfigurator joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            joranConfigurator.doConfigure(location);
        } catch (JoranException e) {
            throw new RuntimeException(e);
        }
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
        return loggerContext.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return loggerContext.getLogger(name);
    }

}