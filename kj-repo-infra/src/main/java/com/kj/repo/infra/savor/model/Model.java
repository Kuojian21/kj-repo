package com.kj.repo.infra.savor.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.kj.repo.infra.savor.annotation.Key;
import com.kj.repo.infra.savor.annotation.Shard;

/**
 * @author kuojian21
 */
@SuppressWarnings({"checkstyle:HiddenField", "checkstyle:ParameterNumber", "unchecked"})
public class Model {
    private static final ConcurrentMap<Class<?>, Model> MODELS = Maps.newConcurrentMap();
    private static Logger logger = LoggerFactory.getLogger(Model.class);
    private final String name;
    private final String table;
    private final List<Property> properties;
    private final Map<String, Property> propertyMap;
    private final Property shardProperty;
    private final List<Property> insertProperties;
    private final List<Property> updateDefProperties;

    public Model(Class<?> clazz) {
        super();
        this.name = clazz.getSimpleName();
        Model pModel = Object.class.equals(clazz.getSuperclass()) ? null : Model.model(clazz.getSuperclass());
        List<Property> tProperties = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                .map(Property::new).collect(Collectors.toList());
        if (pModel != null) {
            tProperties.addAll(pModel.getProperties());
        }
        this.properties = Collections.unmodifiableList(tProperties);
        this.propertyMap = Collections.unmodifiableMap(
                properties.stream().map(p -> Lists.newArrayList(Pair.pair(p.getName(), p), Pair.pair(p.getColumn(), p)))
                        .flatMap(List::stream).distinct().collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
        Shard shard = clazz.getAnnotation(Shard.class);
        if (shard == null) {
            this.table = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.name);
            this.shardProperty = null;
        } else {
            this.table = !Strings.isNullOrEmpty(shard.table()) ? shard.table()
                    : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.name);
            this.shardProperty = !Strings.isNullOrEmpty(shard.shardKey()) ? this.getProperty(shard.shardKey()) : null;
        }
        this.updateDefProperties = Collections.unmodifiableList(
                this.properties.stream().filter(p -> p.updateDef != null).collect(Collectors.toList()));
        this.insertProperties = Collections
                .unmodifiableList(this.properties.stream().filter(Property::isInsert).collect(Collectors.toList()));
    }

    public static Model model(Class<?> clazz) {
        return MODELS.computeIfAbsent(clazz, Model::new);
    }

    public Property getProperty(String name) {
        return this.propertyMap.get(name);
    }

    public List<Property> getProperties(Collection<String> names) {
        return names.stream().map(this.propertyMap::get).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public String getTable() {
        return table;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Map<String, Property> getPropertyMap() {
        return propertyMap;
    }

    public List<Property> getInsertProperties() {
        return insertProperties;
    }

    public Property getShardProperty() {
        return shardProperty;
    }

    public List<Property> getUpdateDefProperties() {
        return updateDefProperties;
    }

    /**
     * @author kuojian21
     */
    public static class Property {

        private final String name;
        private final String column;
        private final Class<?> type;
        private final Field field;
        private final Key key;
        private final boolean insert;
        private final Supplier<Object> insertDef;
        private final Supplier<Object> updateDef;

        public Property(Field f) {
            f.setAccessible(true);
            this.field = f;
            this.name = f.getName();
            this.column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, f.getName());
            this.type = f.getType();
            this.key = f.getAnnotation(Key.class);
            this.insert = this.key == null || this.key.insert();
            this.insertDef = (this.key != null) ? def(this.key.defInsert()) : null;
            this.updateDef = (this.key != null) ? def(this.key.defUpdate()) : null;
        }

        public Supplier<Object> def(String value) {
            if (!Strings.isNullOrEmpty(value)) {
                if (Long.class.equals(this.type)) {
                    return System::currentTimeMillis;
                } else if (java.sql.Date.class.equals(this.type)) {
                    return () -> new java.sql.Date(System.currentTimeMillis());
                } else if (Timestamp.class.equals(this.type)) {
                    return () -> new Timestamp(System.currentTimeMillis());
                }
            }
            return null;
        }

        public Object getOrInsertDef(Object obj) {
            try {
                Object rtn = this.field.get(obj);
                if (rtn == null && this.insertDef != null) {
                    return this.insertDef.get();
                }
                return rtn;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                logger.error("", e);
                return null;
            }
        }

        public Object cast(Object obj) {
            if (obj != null && !this.type.equals(obj.getClass())) {
                if (this.type == Long.class) {
                    return Long.parseLong(obj.toString());
                } else if (this.type == Integer.class) {
                    return Integer.parseInt(obj.toString());
                } else if (this.type == Short.class) {
                    return Short.parseShort(obj.toString());
                } else if (this.type == Byte.class) {
                    return Byte.parseByte(obj.toString());
                } else if (this.type == Double.class) {
                    return Double.parseDouble(obj.toString());
                } else if (this.type == Float.class) {
                    return Float.parseFloat(obj.toString());
                }
            }
            return obj;
        }

        public String getName() {
            return name;
        }

        public String getColumn() {
            return column;
        }

        public Class<?> getType() {
            return type;
        }

        public Field getField() {
            return field;
        }

        public Key getKey() {
            return key;
        }

        public boolean isInsert() {
            return insert;
        }

        public Supplier<Object> getInsertDef() {
            return insertDef;
        }

        public Supplier<Object> getUpdateDef() {
            return updateDef;
        }
    }

}