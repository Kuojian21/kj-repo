package com.kj.repo.infra.el.ognl;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.kj.repo.infra.logger.Log4j2Helper;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author kj
 * Created on 2020-07-30
 */
public class OgnlHelper {
    private static final Logger LOGGER = Log4j2Helper.getLogger(OgnlHelper.class);
    private static final ConcurrentMap<String, Node> EXPR_MAP = Maps.newConcurrentMap();
    public static final DefaultMemberAccess DEFAULT_MEMBER_ACCESS = new DefaultMemberAccess();
    public static final DefaultClassResolver DEFAULT_CLASS_RESOLVER = new DefaultClassResolver();
    public static final DefaultTypeConverter DEFAULT_TYPE_CONVERTER = new DefaultTypeConverter();


    public static <T> T execute(Object root, Map<String, Object> params, String expr, T defaultValue) {
        try {
            OgnlContext ognlContext =
                    (OgnlContext) Ognl.createDefaultContext(root, DEFAULT_MEMBER_ACCESS, DEFAULT_CLASS_RESOLVER,
                            DEFAULT_TYPE_CONVERTER);
            params.forEach(ognlContext::put);
            return (T) EXPR_MAP.computeIfAbsent(expr, key -> {
                try {
                    return Ognl.compileExpression(ognlContext, root, expr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).getAccessor().get(ognlContext, root);
        } catch (Throwable t) {
            LOGGER.info("", t);
            return defaultValue;
        }
    }
}

