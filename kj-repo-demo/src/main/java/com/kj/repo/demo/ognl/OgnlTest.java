package com.kj.repo.demo.ognl;

import com.google.common.collect.ImmutableMap;
import com.kj.repo.infra.utils.el.OgnlUtil;

import ognl.DefaultClassResolver;
import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

/**
 * @author kj
 * Created on 2020-07-28
 */
public class OgnlTest {

    public static void main(String[] args) throws OgnlException, ClassNotFoundException {
        Class.forName("com.kj.repo.test.ognl.OgnlTest");
        Item root = new Item();
        root.setKey("key");
        root.setValue("value");
        OgnlContext ognlContext =
                (OgnlContext) Ognl
                        .createDefaultContext(root, new DefaultMemberAccess(), new DefaultClassResolver(),
                                new DefaultTypeConverter());
        ognlContext.put("item", root);
        System.out.println(Ognl.getValue(Ognl.parseExpression("key"), ognlContext, ognlContext.getRoot()));
        System.out
                .println(Ognl.getValue(Ognl.parseExpression("#item.setKey(\"kj\")"), ognlContext,
                        ognlContext.getRoot()));
        System.out.println(
                Ognl.getValue(Ognl.parseExpression("#item.key"), ognlContext, ognlContext.getRoot(), String.class));
        System.out.println(
                (Object) OgnlUtil.execute(root, ImmutableMap.of(), "com.kj.repo.test.ognl.OgnlTest.class", null));
    }


    public static class Item {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
