package com.kj.repo.infra.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kj.repo.infra.helper.JavaHelper;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class CommonsHelper {

    public static Log getLogger() {
        return getLogger(JavaHelper.stack(2).getClassName());
    }

    public static Log asyncLogger() {
        return getLogger(JavaHelper.stack(2).getClassName());
    }

    public static Log syncLogger() {
        return getLogger("sync." + JavaHelper.stack(2).getClassName());
    }

    public static Log getLogger(Class<?> clazz) {
        return LogFactory.getLog(clazz);
    }

    public static Log getLogger(String name) {
        return LogFactory.getLog(name);
    }

}
