package org.eipgrid.jql.util;

import org.eipgrid.jql.jpa.JpaUtils;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class ClassUtils {
    private static HashMap<Class<?>, Object[]> emptyArrays = new HashMap<>();
    private static Class jpaColumnAnnotationClass = findClassOrNull("javax.persistence.Column");

    public static Collection asCollection(Object obj) {
        if (obj instanceof Collection) {
            return (Collection) obj;
        }
        if (obj instanceof Object[]) {
            return Arrays.asList((Object[]) obj);
        }
        return null;
    }

    public static Class findClassOrNull(String className) {
        try {
            Class c = (Class)Class.forName(className);
            return c;
        } catch (Exception e) {
            return null;
        }
    }


    public static <T> T newInstanceOrNull(String className) {
        try {
            Class<T> c = (Class)Class.forName(className);
            return newInstance(c);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T newInstance(Class<T> c) throws RuntimeException {
        try {
            Constructor constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T)constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> toClass(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>)((ParameterizedType)type).getRawType();
        }
        if (type instanceof WildcardType) {
            return (Class<?>)((WildcardType)type).getUpperBounds()[0];
        }
        if (type instanceof GenericArrayType) {
            return Array.newInstance((Class<?>)((GenericArrayType)type).getGenericComponentType(), 0).getClass();
        }
        return (Class<?>)type;
    }

    public static Class<?> getElementType(Field f) {
        Type t = f.getGenericType();
        if (Collection.class.isAssignableFrom(f.getType())) {
            t = ClassUtils.getFirstGenericParameter(t);
        }
        return ClassUtils.toClass(t);
    }

    public static Type getFirstGenericParameter(Type type) {
        Type[] paramTypes = getGenericParameters(type);
        if (paramTypes == null) {
            return null;
        }
        return paramTypes[0];
    }

    public static Type[] getGenericParameters(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType)type;
        return parameterizedType.getActualTypeArguments();
    }

    public static <T> T[] emptyArray(Class<T> class1) {
        synchronized (emptyArrays) {
            Object[] array = emptyArrays.get(class1);
            if (array == null) {
                array = (Object[])Array.newInstance(class1, 0);
                emptyArrays.put(class1, array);
            }
            return (T[])array;
        }
    }

    private static HashMap<Class<?>, Class<?>> arrayTypes = new HashMap<>();
    public static Class<?> getArrayType(Class<?> itemType) {
        Class<?> arrayType = arrayTypes.get(itemType);
        if (arrayType == null) {
            arrayType = Array.newInstance(itemType, 0).getClass();
            arrayTypes.put(itemType, arrayType);
        }
        return arrayType;
    }

    public static Field findAnnotatedField(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getAnnotation(annotation) != null) {
                return f;
            }
        }
        return findAnnotatedField(clazz.getSuperclass(), annotation);
    }

    public static Field findDeclaredField(Class<?> type, String name) {
        for (Field f : type.getDeclaredFields()) {
            if (f.getName().equals(name)) return f;
        }
        if (type == Object.class) return null;
        return findDeclaredField(type.getSuperclass(), name);
    }

    public static Field findPublicField(Class<?> type, String name) {
        for (Field f : type.getFields()) {
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    public static Method findPublicMethod(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    public static ArrayList<Field> getFields(Class entityType, int excludeModifier) {
        ArrayList<Field> fields = new ArrayList<>();
        getFields(fields, entityType, excludeModifier);
        return fields;
    }

    public static void getFields(List<Field> columns, Class entityType, int excludeModifier) {
        Class<?> superClass = entityType.getSuperclass();
        if (superClass != Object.class) {
            getFields(columns, superClass, excludeModifier);
        }
        for (Field f : entityType.getDeclaredFields()) {
            if ((f.getModifiers() & excludeModifier) != 0) continue;
            columns.add(f);
        }
    }

    public static boolean resolveNullable(Field f) {
        for (Annotation a : f.getAnnotations()) {
            Class clazz = a.annotationType();
            if (clazz == jpaColumnAnnotationClass) {
                if (!JpaUtils.isNullable(f, true)) {
                    return false;
                }
            }
            else if (clazz.getSimpleName().endsWith("NotNull")) {
                return false;
            }
        }
        return true;
    }


    public static Class<?> getBoxedType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            clazz = primitiveBoxes.get(clazz);
        }
        return clazz;
    }

    private static final HashMap<Class<?>, Class<?>> primitiveBoxes = new HashMap<>();
    static {
        primitiveBoxes.put(boolean.class, Boolean.class);
        primitiveBoxes.put(byte.class, Byte.class);
        primitiveBoxes.put(char.class, Character.class);
        primitiveBoxes.put(short.class, Short.class);
        primitiveBoxes.put(int.class, Integer.class);
        primitiveBoxes.put(long.class, Long.class);
        primitiveBoxes.put(float.class, Float.class);
        primitiveBoxes.put(double.class, Double.class);
    }


}