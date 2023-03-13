package org.eipgrid.jql.jdbc.timescale;

import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.ClassUtils;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;

public abstract class TSDBRepository<ENTITY, ID> extends JpaTable<ENTITY, ID> {

    private final String timeKeyColumnName;
    private final JdbcStorage storage;

    public TSDBRepository(JdbcStorage storage, Class<ENTITY> entityType, String timeKeyColumnName) {
        super(storage, entityType);
        this.timeKeyColumnName = timeKeyColumnName;

        this.storage = storage;
        QSchema schema = storage.loadSchema(getEntityType());
        try {
            new Initializer(schema).initializeTSDB(schema);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTimeKeyColumnName() {
        return timeKeyColumnName;
    }

    private class Initializer extends TSDBHelper {
        private final QSchema schema;

        public Initializer(QSchema schema) {
            super(storage, schema.getTableName());
            this.schema = schema;
        }

        public HashMap<String, AggregateType> resolveAggregationTypeMap() {
            HashMap<String, AggregateType> aggTypeMap = new HashMap<>();
            for (QColumn col : schema.getWritableColumns()) {
                AggregateType aggType = resolveAggregationType(col);
                String col_name = col.getPhysicalName();
                aggTypeMap.put(col_name, aggType);
            }
            return aggTypeMap;
        }

        private AggregateType resolveAggregationType(QColumn col) {
            Class entityType = getEntityType();
            Field f = ClassUtils.findDeclaredField(entityType, col.getJsonKey());
            if (f == null) {
                return AggregateType.None;
            }
            Aggregate c = f.getAnnotation(Aggregate.class);
            if (c != null) {
                return c.value();
            }
            if (JsType.of(col.getValueType()) == JsType.Float) {
                return AggregateType.Mean;
            }
            return AggregateType.None;
        }

        @Override
        protected String generateDDL(String tableName) {
            throw new RuntimeException("must not reach here!");
        }
    }
}