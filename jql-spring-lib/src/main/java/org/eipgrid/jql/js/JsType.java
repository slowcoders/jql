package org.eipgrid.jql.js;

import com.fasterxml.jackson.databind.JsonNode;
import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public enum JsType {
    Boolean,
    Integer,
    Float,
    Text,
    Date,
    Time,
    Timestamp,
    Object,
    Array;

    public boolean isPrimitive() {
        return this.ordinal() < Object.ordinal();
    }

    public static JsType of(Field f) {
        Class javaType = f.getType();
        if (javaType.isEnum()) {
            Enumerated e = f.getAnnotation(Enumerated.class);
            if (e != null && e.value() == EnumType.STRING) {
                return JsType.Text;
            }
            else {
                return JsType.Integer;
            }
        }
        return JsType.of(javaType);
    }

    static HashMap<Class, JsType> typeMap = new HashMap<>();
    
    static {
        typeMap.put(java.sql.Timestamp.class, JsType.Timestamp);
        typeMap.put(java.util.Date.class, JsType.Timestamp);
        typeMap.put(OffsetDateTime.class, JsType.Timestamp);
        typeMap.put(Instant.class, JsType.Timestamp);
        typeMap.put(ZonedDateTime.class, JsType.Timestamp);

        typeMap.put(java.sql.Time.class, JsType.Time);
        typeMap.put(java.sql.Date.class, JsType.Time);

        typeMap.put(boolean.class, JsType.Boolean);
        typeMap.put(byte.class, JsType.Integer);
        typeMap.put(char.class, JsType.Integer);
        typeMap.put(short.class, JsType.Integer);
        typeMap.put(int.class, JsType.Integer);
        typeMap.put(long.class, JsType.Integer);
        typeMap.put(float.class, JsType.Float);
        typeMap.put(double.class, JsType.Float);

        typeMap.put(Boolean.class, JsType.Boolean);
        typeMap.put(Byte.class, JsType.Integer);
        typeMap.put(Character.class, JsType.Integer);
        typeMap.put(Short.class, JsType.Integer);
        typeMap.put(Integer.class, JsType.Integer);
        typeMap.put(Long.class, JsType.Integer);
        typeMap.put(Float.class, JsType.Float);
        typeMap.put(Double.class, JsType.Float);
        
        typeMap.put(String.class, JsType.Text);
    }
    
    public static JsType of(Class javaType) {
        if (javaType.isArray() || java.util.Collection.class.isAssignableFrom(javaType)) {
            return JsType.Array;
        }
        
        if (Map.class.isAssignableFrom(javaType)) {
            return JsType.Object;
        }
        
        JsType type = typeMap.get(javaType);
        if (type == null) type = JsType.Object;
        return type;
    }

}
