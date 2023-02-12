package org.eipgrid.jql.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Field;
import java.util.Collection;

public abstract class QColumn {
    private final QSchema schema;
    private final String physicalName;
    private final Class valueType;
    protected QColumn(QSchema schema, String physicalName, Class valueType) {
        this.schema = schema;
        this.physicalName = physicalName;
        this.valueType = valueType;
    }

    public final QSchema getSchema() {
        return schema;
    }

    public final Class getValueType() {
        return valueType;
    }

    public final String getPhysicalName() {
        return physicalName;
    }

    public boolean isJsonNode() {
        return this.valueType == JsonNode.class;
    }

    public abstract String getJsonKey();

    public Field getMappedOrmField() { return null; }

    protected void setMappedOrmField(Field f) {}

    //===========================================================
    // Overridable Properties
    //-----------------------------------------------------------

    public boolean isReadOnly() {
        return false;
    }

    public boolean isNullable() {
        return true;
    }

    public boolean isAutoIncrement() {
        return false;
    }

    public boolean isPrimaryKey() { return false; }

    public boolean isForeignKey() { return false; }

    public String getLabel() {
        return null;
    }

    public QColumn getJoinedPrimaryColumn() {
        return null;
    }

    @Override
    public int hashCode() {
        return physicalName.hashCode();
    }

    public String toString() { return getSchema().getSimpleTableName() + "::" + this.getJsonKey()+ "<" + physicalName + ">"; }

}
