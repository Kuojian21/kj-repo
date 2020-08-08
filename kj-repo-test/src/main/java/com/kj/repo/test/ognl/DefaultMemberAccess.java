package com.kj.repo.test.ognl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;

import ognl.MemberAccess;

/**
 * @author kj
 * Created on 2020-07-28
 */
public class DefaultMemberAccess implements MemberAccess {
    private boolean allowPrivate = true;
    private boolean allowProtected = true;
    private boolean allowDefault = true;

    public DefaultMemberAccess() {
    }

    public DefaultMemberAccess(boolean allowPrivate, boolean allowProtected, boolean allowDefault) {
        this.allowPrivate = allowPrivate;
        this.allowProtected = allowProtected;
        this.allowDefault = allowDefault;
    }

    @Override
    public Object setup(Map context, Object target, Member member, String propertyName) {
        new Exception().printStackTrace();
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
