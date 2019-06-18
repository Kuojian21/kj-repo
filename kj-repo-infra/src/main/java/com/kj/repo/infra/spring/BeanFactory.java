package com.kj.repo.infra.spring;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author kuojian21
 */
@Service
public final class BeanFactory {

    private static volatile GenericApplicationContext applicationContext;


    public BeanFactory() {
    }

    public static void init() {
        if (applicationContext == null) {
            synchronized (BeanFactory.class) {
                if (applicationContext == null) {
                    try {
                        applicationContext = new GenericApplicationContext(
                                new ClassPathXmlApplicationContext("classpath*:spring/*.xml"));
                        applicationContext.refresh();
                    } catch (RuntimeException e) {
                        throw e;
                    }
                }
            }
        }

    }

    public static <T> T getBean(Class<T> clazz) {
        init();
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(Class<T> clazz, Object... args) {
        init();
        return applicationContext.getBean(clazz, args);
    }

    public static <T> T getBean(String beanName, Class<T> tClass) {
        init();
        return applicationContext.getBean(beanName, tClass);
    }

    public static <T> Map<String, T> getBeans(Class<T> type) {
        init();
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, type);
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof GenericApplicationContext) {
            BeanFactory.applicationContext = (GenericApplicationContext) applicationContext;
        } else {
            BeanFactory.applicationContext = new GenericApplicationContext(applicationContext);
            BeanFactory.applicationContext.refresh();
        }
    }
}
