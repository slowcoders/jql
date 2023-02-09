package org.eipgrid.jql.schema;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.lang.reflect.Field;
import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public abstract class QSchema {
    private final SchemaLoader schemaLoader;
    private final String tableName;
    private final Class<?> entityType;

    private final boolean isJPASchema;

    private List<QColumn> pkColumns;
    private List<QColumn> allColumns;
    private List<QColumn> primitiveColumns;
    private List<QColumn> objectColumns;
    private List<QColumn> writableColumns;
    private Map<String, QColumn> columnMap = new HashMap<>();
    private HashMap<String, QJoin> entityJoinMap;

    public QSchema(SchemaLoader schemaLoader, String tableName, Class<?> entityType) {
        this.tableName = tableName;
        this.schemaLoader = schemaLoader;
        this.entityType = entityType;
        this.isJPASchema = !Map.class.isAssignableFrom(entityType);
    }

    public final SchemaLoader getSchemaLoader() {
        return schemaLoader;
    }

    public final boolean isJPARequired() { return this.isJPASchema; }

    public final Class<?> getEntityType() { return entityType; }

    public String getTableName() {
        return this.tableName;
    }

    public String getSimpleTableName() {
        return this.tableName.substring(this.tableName.indexOf('.') + 1);
    }


    public List<QColumn> getReadableColumns() {
        return this.allColumns;
    }

    public List<QColumn> getPrimitiveColumns() {
        return this.primitiveColumns;
    }

    public List<QColumn> getObjectColumns() {
        return this.objectColumns;
    }

    public List<QColumn> getWritableColumns() {
        return (List)writableColumns;
    }

    public List<QColumn> getPKColumns() {
        return this.pkColumns;
    }

    public QJoin getEntityJoinBy(String jsonKey) {
        return getEntityJoinMap().get(jsonKey);
    }

    public QColumn findColumn(String key) throws IllegalArgumentException {
        QColumn ci = columnMap.get(key);
        if (ci == null) {
            ci = columnMap.get(key.toLowerCase());
        }
        return ci;
    }

    public QColumn getColumn(String key) throws IllegalArgumentException {
        QColumn ci = findColumn(key);
        if (ci == null) {
            throw new IllegalArgumentException("unknown column [" + this.tableName + "::" + key + "]");
        }
        return ci;
    }

    public boolean hasColumn(String key) {
        return columnMap.get(key) != null;
    }

    protected void init(ArrayList<? extends QColumn> columns, Class<?> ormType) {
        ArrayList<QColumn> writableColumns = new ArrayList<>();
        ArrayList<QColumn> allColumns = new ArrayList<>();
        ArrayList<QColumn> primitiveColumns = new ArrayList<>();
        ArrayList<QColumn> objectColumns = new ArrayList<>();
        List<QColumn> pkColumns = new ArrayList<>();

        for (QColumn ci: columns) {
            String colName = ci.getPhysicalName().toLowerCase();
            this.columnMap.put(colName, ci);

            if (ci.isPrimaryKey()) {
                pkColumns.add(ci);
            }
            else {
                allColumns.add(ci);
            }

            if (!ci.isForeignKey()) {
                if (ci.getValueType().isPrimitive()) {
                    primitiveColumns.add(ci);
                }
                else {
                    objectColumns.add(ci);
                }
                if (!ci.isReadOnly()) {
                    writableColumns.add(ci);
                }
            }
        }

        allColumns.addAll(0, pkColumns);
        this.allColumns = Collections.unmodifiableList(allColumns);
        this.writableColumns = Collections.unmodifiableList(writableColumns);
        this.primitiveColumns = Collections.unmodifiableList(primitiveColumns);
        this.objectColumns = objectColumns.size() == 0 ? Collections.EMPTY_LIST : Collections.unmodifiableList(objectColumns);
        this.initJsonKeys(ormType);
        if (pkColumns.size() == 0) {
            pkColumns = this.allColumns;
        }
        this.pkColumns = Collections.unmodifiableList(pkColumns);
    }

    protected void mapColumn(QColumn column, Field f) {
        column.setMappedOrmField(f);
    }

    protected void initJsonKeys(Class<?> ormType) {
        for (QColumn ci : allColumns) {
            String fieldName = ci.getJsonKey();
            columnMap.put(fieldName, ci);
        }
    }

    public Map<String, Object> splitUnknownProperties(Map<String, Object> metric)  {
        HashMap<String, Object> unknownProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : metric.entrySet()) {
            String key = entry.getKey();
            if (!this.columnMap.containsKey(key) &&
                !this.columnMap.containsKey(key.toLowerCase())) {
                unknownProperties.put(key, entry.getValue());
            }
        }
        for (String key : unknownProperties.keySet()) {
            metric.remove(key);
        }
        return unknownProperties;
    }


    public HashMap<String, QJoin> getEntityJoinMap() {
        if (this.entityJoinMap == null) {
            this.entityJoinMap = schemaLoader.loadJoinMap(this);
        }
        return this.entityJoinMap;
    }
    //==========================================================================
    // Attribute Name Conversion
    //--------------------------------------------------------------------------

    public static String getJavaFieldName(QColumn column) {
        String name = column.getJsonKey();
        int idx = name.indexOf('.');
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return name;
    }


    public String getPhysicalColumnName(String fieldName) {
        return this.columnMap.get(fieldName).getPhysicalName();
    }

    public String getLogicalAttributeName(String columnName) {
        return this.columnMap.get(columnName).getJsonKey();
    }

    public String[] getPhysicalColumnNames(String[] fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) return null;
        String[] out = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i ++) {
            String key = fieldNames[i];
            out[i] = getPhysicalColumnName(key);
        }
        return out;
    }

    public List<String> getPhysicalColumnNames(Iterable<String> fieldNames) {
        ArrayList<String> out = new ArrayList<>();
        for (String key : fieldNames) {
            out.add(getPhysicalColumnName(key));
        }
        return out;
    }

    public boolean isUniqueConstrainedColumnSet(List<QColumn> fkColumns) {
        return false;
    }

    public String toString() {
        return this.tableName;
    }

    public String getNamespace() {
        String tableName = this.getTableName();
        int p = tableName.lastIndexOf('.');
        return p < 0 ? null : tableName.substring(0, p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QSchema schema = (QSchema) o;
        return tableName.equals(schema.tableName);
    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
    }

    public boolean hasGeneratedId() {
        throw new RuntimeException("not impl");
    }

    public boolean hasOnlyForeignKeys() {
        for (QColumn col : getReadableColumns()) {
            if (col.getJoinedPrimaryColumn() == null) {
                return false;
            }
        }
        return true;
    }


    public abstract <ID, ENTITY> ID getEnityId(ENTITY entity);
}
