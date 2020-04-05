package com.kj.repo.infra.serializer.xml;

import java.io.InputStream;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author kj
 * Created on 2020-03-31
 */
public class XStreamHelper {

    public static <T> T from(InputStream is, Map<String, Class<?>> aliasMap) {
        XStream xstream = new XStream(new DomDriver());
        aliasMap.forEach(xstream::alias);
        return (T) xstream.fromXML(is);
    }

    public static <T> T from(InputStream is, Class<?> clazz) {
        XStream xstream = new XStream(new DomDriver());
        xstream.processAnnotations(clazz);
        return (T) xstream.fromXML(is);
    }

}
