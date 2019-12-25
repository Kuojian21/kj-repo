package com.kj.repo.infra.savor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author kj
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {

    boolean insert() default true;

    String defInsert() default "";

    String defUpdate() default "";
}