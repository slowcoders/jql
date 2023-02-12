package org.eipgrid.jql.csv;

import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.util.ClassUtils;

import java.lang.reflect.Field;

public class CsvColumn {
    private final JsType valueType;
    private final Field field;
    private final Class<?> elementType;
    private boolean isNullable;

    public CsvColumn(Field f) {
        this.field = f;
        this.valueType = JsType.of(f);
        if (this.valueType == JsType.Array) {
            this.elementType = ClassUtils.getElementType(f);
            this.isNullable = false;
        } else {
            this.elementType = f.getType();
            this.isNullable = ClassUtils.resolveNullable(f);
        }
    }

    public final String getName() {
        return field.getName();
    }

    public Class getElementType() {
        return elementType;
    }

    boolean isNullable() {
        return this.isNullable;
    }

    public JsType getValueType() {
        return this.valueType;
    }
}
