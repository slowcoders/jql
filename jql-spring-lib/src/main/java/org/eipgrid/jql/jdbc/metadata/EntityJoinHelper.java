package org.eipgrid.jql.jdbc.metadata;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QJoin;

import java.util.*;

class EntityJoinHelper extends HashMap<QSchema, QJoin> {
    private String tableName;
    private HashSet<QSchema> conflictMappings = new HashSet<>();
    public EntityJoinHelper(JdbcSchema pkSchema) {
        this.tableName = pkSchema.generateEntityClassName().toLowerCase();
    }

    public void validate() {
        for (QSchema schema : conflictMappings) {
            super.remove(schema);
        }
    }
    
    public QJoin put(QSchema schema, QJoin childJoin) {
        QJoin oldJoin = super.put(schema, childJoin);
        if (oldJoin != null && oldJoin != childJoin) {
            if (oldJoin.getJsonKey().toLowerCase().startsWith(tableName)) {
                super.put(schema, oldJoin);
                return oldJoin;
            } else if (childJoin.getJsonKey().toLowerCase().equals(tableName)) {
                // do nothing;
                return oldJoin;
            }
            conflictMappings.add(schema);
        }
        return oldJoin;
    }

    public Map<String, QJoin> createJoinMap(JdbcSchema baseSchema) {
        Map<String, QJoin> res = baseSchema.getEntityJoinMap_unsafe();
        if (res != null) {
            return res;
        }

        JdbcSchemaLoader.JoinMap joinMap = new JdbcSchemaLoader.JoinMap();
        for (List<QColumn> fkColumns : baseSchema.getForeignKeyConstraints().values()) {
            joinMap.add(baseSchema, fkColumns, null);
        }
        for (QJoin fkJoin : this.values()) {
            List<QColumn> fkColumns = fkJoin.getForeignKeyColumns();
            joinMap.add(baseSchema, fkColumns, null);

            if (fkColumns.get(0).getSchema().hasOnlyForeignKeys()) {
                JdbcSchema exSchema = (JdbcSchema) fkJoin.getBaseSchema();
                Collection<String> fkConstraints = exSchema.getForeignKeyConstraints().keySet();
                for (String fkConstraint : fkConstraints) {
                    QJoin j2 = exSchema.getJoinByForeignKeyConstraints(fkConstraint);
                    if (j2 != fkJoin) {// && linkedSchema != baseSchema) {
                        joinMap.add(baseSchema, fkColumns, j2);
                    }
                }
            }
        }
        return joinMap;
    }
}
