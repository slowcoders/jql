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


    public static JsType of(Class javaType) {
        if (javaType.getAnnotation(MappedSuperclass.class) != null
                ||  javaType.getAnnotation(Embeddable.class) != null) {
            return JsType.Object;
        }
        if (javaType == Object.class ||
                JsonNode.class.isAssignableFrom(javaType) ||
                Map.class.isAssignableFrom(javaType)) {
            return JsType.Object;
        }
        if (java.util.Collection.class.isAssignableFrom(javaType)) {
            return JsType.Array;
        }
        if (javaType == java.sql.Timestamp.class) {
            return JsType.Timestamp;
        }
        if (javaType == java.util.Date.class) {
            return JsType.Timestamp;
        }
        if (javaType == OffsetDateTime.class) {
            return JsType.Timestamp;
        }
        if (javaType == Instant.class || javaType == ZonedDateTime.class) {
            return JsType.Timestamp;
        }

        if (javaType == java.sql.Time.class) {
            return JsType.Time;
        }
        if (javaType == java.sql.Date.class) {
            return JsType.Date;
        }

        javaType = ClassUtils.getBoxedType(javaType);
        if (javaType == Boolean.class || Number.class.isAssignableFrom(javaType)) {
            if (javaType == Float.class || javaType == Double.class) {
                return JsType.Float;
            }
            return JsType.Integer;
        }
        return javaType == String.class ? JsType.Text : JsType.Object;
    }

}
