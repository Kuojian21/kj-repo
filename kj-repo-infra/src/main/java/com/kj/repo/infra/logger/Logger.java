package com.kj.repo.infra.logger;

/**
 * @author kj
 * Created on 2020-08-30
 */
public interface Logger {
    void debug(String message, Object... params);

    void debug(String message, Throwable e);

    void info(String message, Object... params);

    void info(String message, Throwable e);

    void warn(String message, Object... params);

    void warn(String message, Throwable e);

    void error(String message, Object... params);

    void error(String message, Throwable e);

    void fatal(String message, Object... params);

    void fatal(String message, Throwable e);
}
