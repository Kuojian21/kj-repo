package com.kj.repo.infra.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kj.repo.infra.utils.JavaUtil;

/**
 * @author kj
 * Created on 2020-03-16
 */
public class CommonsHelper {

    public static Log getLogger() {
        return getLogger(JavaUtil.stack(2).getClassName());
    }

    public static Log asyncLogger() {
        return getLogger(JavaUtil.stack(2).getClassName());
    }

    public static Log syncLogger() {
        return getLogger("sync." + JavaUtil.stack(2).getClassName());
    }

    public static Log getLogger(Class<?> clazz) {
        return LogFactory.getLog(clazz);
    }

    public static Log getLogger(String name) {
        return LogFactory.getLog(name);
    }

}
