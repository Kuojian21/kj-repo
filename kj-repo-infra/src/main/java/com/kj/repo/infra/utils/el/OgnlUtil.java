package com.kj.repo.infra.utils.el;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.kj.repo.infra.logger.Log4j2Helper;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
import ognl.MemberAccess;
import ognl.Node;
import ognl.Ognl;
import ognl.OgnlContext;

/**
 * @author kj
 * Created on 2020-07-30
 */
public class OgnlUtil {
    public static final DefaultMemberAccess DEFAULT_MEMBER_ACCESS = new DefaultMemberAccess();
    public static final DefaultClassResolver DEFAULT_CLASS_RESOLVER = new DefaultClassResolver();
    public static final DefaultTypeConverter DEFAULT_TYPE_CONVERTER = new DefaultTypeConverter();
    private static final Logger LOGGER = Log4j2Helper.getLogger(OgnlUtil.class);
    private static final ConcurrentMap<String, Node> EXPR_MAP = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked")
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

class DefaultMemberAccess implements MemberAccess {
    private boolean allowPrivate = true;
    private boolean allowProtected = true;
    private boolean allowDefault = true;

    public DefaultMemberAccess() {
        this(false, false, false);
    }

    public DefaultMemberAccess(boolean allowPrivate, boolean allowProtected, boolean allowDefault) {
        this.allowPrivate = allowPrivate;
        this.allowProtected = allowProtected;
        this.allowDefault = allowDefault;
    }

    @Override
    public Object setup(Map context, Object target, Member member, String propertyName) {
        Object result = null;
        if (isAccessible(context, target, member, propertyName)) {
            AccessibleObject accessible = (AccessibleObject) member;
            if (!accessible.isAccessible()) {
                result = Boolean.TRUE;
                accessible.setAccessible(true);
            }
        }
        return result;
    }

    @Override
    public void restore(Map context, Object target, Member member, String propertyName, Object state) {
        if (state != null) {
            ((AccessibleObject) member).setAccessible((Boolean) state);
        }
    }

    @Override
    public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
        int modifiers = member.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            return true;
        } else if (Modifier.isPrivate(modifiers)) {
            return allowPrivate;
        } else if (Modifier.isProtected(modifiers)) {
            return allowProtected;
        } else {
            return allowDefault;
        }
    }
}

