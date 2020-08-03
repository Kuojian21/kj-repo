package com.kj.repo.infra.ognl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author kj
 * Created on 2020-07-30
 */
public class OgnlHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OgnlHelper.class);
    private static final DefaultMemberAccess MEMBER_ACCESS = new DefaultMemberAccess();
    private static final DefaultClassResolver CLASS_RESOLVER = new DefaultClassResolver();
    private static final DefaultTypeConverter TYPE_CONVERTER = new DefaultTypeConverter();

    public static <T> T execute(Object obj, Map<String, Object> params, String expr, T defaultValue) {
        try {
            OgnlContext ognlContext =
                    (OgnlContext) Ognl.createDefaultContext(obj, MEMBER_ACCESS, CLASS_RESOLVER, TYPE_CONVERTER);
            params.forEach(ognlContext::put);
            return (T) Ognl.getValue(Ognl.parseExpression(expr), ognlContext, ognlContext.getRoot());
        } catch (Throwable t) {
            LOGGER.info("", t);
            return defaultValue;
        }
    }
}

