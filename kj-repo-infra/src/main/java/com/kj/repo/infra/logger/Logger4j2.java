package com.kj.repo.infra.logger;

/**
 * @author kj
 * Created on 2020-08-30
 */
public class Logger4j2 implements Logger {
    private final org.apache.logging.log4j.Logger logger;

    public Logger4j2(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String message, Object... params) {
        logger.debug(message, params);
    }

    @Override
    public void debug(String message, Throwable e) {
        logger.debug(message, e);
    }

    @Override
    public void info(String message, Object... params) {
        logger.info(message, params);
    }

    @Override
    public void info(String message, Throwable e) {
        logger.info(message, e);
    }

    @Override
    public void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    @Override
    public void warn(String message, Throwable e) {
        logger.warn(message, e);
    }

    @Override
    public void error(String message, Object... params) {
        logger.error(message, params);
    }

    @Override
    public void error(String message, Throwable e) {
        logger.error(message, e);
    }

    @Override
    public void fatal(String message, Object... params) {
        logger.fatal(message, params);
    }

    @Override
    public void fatal(String message, Throwable e) {
        logger.fatal(message, e);
    }
}
