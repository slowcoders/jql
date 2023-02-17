package org.eipgrid.jql.jpa;

import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public interface JpaUtils {

    static List<Field> findIdFields(Class<?> clazz) {
        ArrayList<Field> idFields = new ArrayList<Field>() {
            public boolean add(Field f) {
                if (!isIdField(f)) {
                    return false;
                }
                return super.add(f);
            }
        };

        ClassUtils.getFields(idFields, clazz, Modifier.STATIC | Modifier.TRANSIENT);
        return idFields;
    }

    static List<Field> getColumnFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<Field>() {
            public boolean add(Field f) {
                OneToMany o2m; OneToOne o2o; ManyToMany m2m; ManyToOne m2o;

                if (f.getAnnotation(Transient.class) != null) return false;

                if (((((o2m = f.getAnnotation(OneToMany.class))) != null) && (o2m.mappedBy().length() > 0)) ||
                        ((((o2o = f.getAnnotation(OneToOne.class))) != null) && (o2o.mappedBy().length() > 0)) ||
                        ((((m2m = f.getAnnotation(ManyToMany.class))) != null) && (m2m.mappedBy().length() > 0))) {
                    return false;
                }
                return super.add(f);
            }
        };
        ClassUtils.getFields(fields, clazz, Modifier.STATIC | Modifier.TRANSIENT);
        return fields;
    }

    static List<Field> getCacheableFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<Field>() {
            public boolean add(Field f) {
                if (f.getAnnotation(Transient.class) != null) return false;
                return super.add(f);
            }
        };
        ClassUtils.getFields(fields, clazz, Modifier.STATIC | Modifier.TRANSIENT);
        return fields;
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
