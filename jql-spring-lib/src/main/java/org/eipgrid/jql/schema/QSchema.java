package org.eipgrid.jql.schema;

import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;

import java.lang.reflect.Field;
import java.util.*;

public abstract class QSchema {
    private final String tableName;
    private final Class<?> entityType;

    private final boolean isJPASchema;

    private List<QColumn> pkColumns;
    private List<QColumn> allColumns;
    private List<QColumn> leafColumns;
    private List<QColumn> objectColumns;
    private List<QColumn> writableColumns;
    private Map<String, QColumn> columnMap = new HashMap<>();
    private boolean hasGeneratedId;

    public QSchema(String tableName, Class<?> entityType) {
        this.tableName = tableName;
        this.entityType = entityType;
        this.isJPASchema = !JqlRepository.rawEntityType.isAssignableFrom(entityType);
    }

    public abstract JqlStorage getStorage();

    public final boolean isJPARequired() { return this.isJPASchema; }

    public final Class<?> getEntityType() { return entityType; }

    public Class<?> getIdType() { return Object.class; }

    public final String getTableName() {
        return this.tableName;
    }

    public final String getSimpleName() {
        return this.tableName.substring(this.tableName.indexOf('.') + 1);
    }

    public final String generateEntityClassName() {
        return getStorage().toEntityClassName(this.getSimpleName(), true);
    }


    public List<QColumn> getReadableColumns() {
        return this.allColumns;
    }

    public List<QColumn> getLeafColumns() {
        return this.leafColumns;
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

        boolean hasGeneratedId = false;
        for (QColumn ci: columns) {
            this.columnMap.put(ci.getPhysicalName().toLowerCase(), ci);

            if (ci.isPrimaryKey()) {
                pkColumns.add(ci);
                hasGeneratedId |= ci.isAutoIncrement();
            }
            else {
                allColumns.add(ci);
            }

            if (!ci.isForeignKey()) {
                if (ci.isJsonNode()) {
                    objectColumns.add(ci);
                }
                else {
                    primitiveColumns.add(ci);
                }
                if (!ci.isReadOnly()) {
                    writableColumns.add(ci);
                }
            }
        }

        allColumns.addAll(0, pkColumns);
        this.hasGeneratedId = hasGeneratedId;
        this.allColumns = Collections.unmodifiableList(allColumns);
        this.writableColumns = Collections.unmodifiableList(writableColumns);
        this.leafColumns = Collections.unmodifiableList(primitiveColumns);
        this.objectColumns = objectColumns.size() == 0 ? Collections.EMPTY_LIST : Collections.unmodifiableList(objectColumns);
        this.initJsonKeys(ormType);
        if (pkColumns.size() == 0) {
            pkColumns = this.allColumns;
            markAllColumnsToPK(pkColumns);
        }
        this.pkColumns = Collections.unmodifiableList(pkColumns);
    }

    protected void markAllColumnsToPK(List<QColumn> pkColumns) {
        throw new RuntimeException("not implemented");
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


    public Map<String, QJoin> getEntityJoinMap() {
        return Collections.EMPTY_MAP;
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
        return this.hasGeneratedId;
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
