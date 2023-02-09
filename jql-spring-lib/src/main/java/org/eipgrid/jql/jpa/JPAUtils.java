package org.eipgrid.jql.jpa;

import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.Id;
import javax.persistence.IdClass;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public interface JPAUtils {
    static List<Field> findIdFields(Class<?> clazz) {
        ArrayList<Field> idFields = new ArrayList<>();
        for (Field f : ClassUtils.getInstanceFields(clazz, true)) {
            if (f.getAnnotation(Id.class) != null ||
                f.getAnnotation(org.springframework.data.annotation.Id.class) != null) {
                idFields.add(f);
            }
        }
        return idFields;
    }

    static boolean isIdField(Field f) {
        return f.getAnnotation(Id.class) != null
            || f.getAnnotation(org.springframework.data.annotation.Id.class) != null;
    }

    static Class<?> getIdType(Class<?> entityType) {
        IdClass idClass = entityType.getAnnotation(IdClass.class);
        if (idClass != null) {
            return idClass.value();
        }
        return null;
    }
}
