package com.kj.repo.infra.tool;

import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * @author kj
 */
@SuppressWarnings({"checkstyle:HiddenField", "checkstyle:ParameterNumber"})
public class TLJson {
    public static final ConcurrentMap<Class<?>, Model> FILEDMAPS = Maps.newConcurrentMap();
    public static final Set<Class<?>> PRIME_CLASSES =
            Sets.newHashSet(Long.class, Integer.class, Short.class, Byte.class, Float.class, Double.class, Boolean.class, Character.class);

    private static Logger logger = LoggerFactory.getLogger(TLJson.class);

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            sb.append("[");
            Object[] objs = (Object[]) obj;
            if (objs.length > 0) {
                sb.append(Joiner.on(",")
                        .join(Lists.newArrayList(objs).stream().map(TLJson::toJson).collect(Collectors.toList())));
            }
            sb.append("]");
        } else if (obj instanceof Collection<?>) {
            sb.append("[");
            sb.append(Joiner.on(",")
                    .join(((Collection<?>) obj).stream().map(TLJson::toJson).collect(Collectors.toList())));
            sb.append("]");
        } else if (clazz.isPrimitive() || PRIME_CLASSES.contains(clazz)) {
            sb.append(obj.toString());
        } else if (clazz.equals(String.class)) {
            sb.append("\"");
            sb.append(obj.toString());
            sb.append("\"");
        } else {
            sb.append("{");
            Model model = FILEDMAPS.computeIfAbsent(clazz, Model::new);
            model.properties.forEach(p -> {
                sb.append("\"").append(p.getName()).append("\":").append(toJson(p.get(obj)));
            });
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * @author kj
     */
    public static class Model {
        private final String name;
        private final List<Property> properties;
        private final Map<String, Property> propertyMap;

        public Model(Class<?> clazz) {
            super();
            this.name = clazz.getSimpleName();
            List<PropertyDescriptor> descriptors = Lists.newArrayList();
            try {
                descriptors.addAll(Arrays.asList(Introspector.getBeanInfo(clazz).getPropertyDescriptors()));
            } catch (Exception e) {
                logger.error("", e);
            }
            Map<String, PropertyDescriptor> descriptorMap = descriptors.stream()
                    .collect(Collectors.toMap(FeatureDescriptor::getName, d -> d));
            List<Property> properties = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                    .map(f -> new Property(f, descriptorMap.get(f.getName()))).collect(Collectors.toList());
            Map<String, Property> propertyMap = properties.stream()
                    .collect(Collectors.toMap(Property::getName, p -> p));
            descriptorMap.forEach((k, v) -> {
                if (propertyMap.putIfAbsent(k, new Property(v)) != null) {
                    properties.add(propertyMap.get(k));
                }
            });
            this.properties = Collections.unmodifiableList(properties);
            this.propertyMap = Collections.unmodifiableMap(propertyMap);

        }

        public Property getProperty(String name) {
            return this.propertyMap.get(name);
        }

		public String getName() {
			return name;
		}

		public List<Property> getProperties() {
			return properties;
		}

		public Map<String, Property> getPropertyMap() {
			return propertyMap;
		}

    }

    /**
     * @author kj
     */
    public static class Property {

        private final String name;
        private final Class<?> type;
        private final Field field;
        private final PropertyDescriptor descriptor;

        public Property(Field f, PropertyDescriptor descriptor) {
            f.setAccessible(true);
            this.field = f;
            this.name = f.getName();
            this.type = f.getType();
            this.descriptor = descriptor;
        }

        public Property(PropertyDescriptor descriptor) {
            this.field = null;
            this.name = descriptor.getName();
            this.type = descriptor.getPropertyType();
            this.descriptor = descriptor;
        }

        public Object get(Object obj) {
            try {
                if (this.field != null) {
                    return this.field.get(obj);
                } else {
                    return this.descriptor.getReadMethod().invoke(obj);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                logger.error("", e);
                return null;
            }
        }

        public void set(Object obj, Object value) {
            try {
                if (this.field != null) {
                    this.field.set(obj, value);
                } else {
                    this.descriptor.getWriteMethod().invoke(obj, value);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                logger.error("", e);
            }
        }

		public String getName() {
			return name;
		}

		public Class<?> getType() {
			return type;
		}

		public Field getField() {
			return field;
		}

		public PropertyDescriptor getDescriptor() {
			return descriptor;
		}

    }

}
