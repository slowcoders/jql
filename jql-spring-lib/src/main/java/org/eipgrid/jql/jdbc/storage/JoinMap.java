package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;

import java.util.*;

class JoinMap extends HashMap<String, QJoin> {

    public void add(JdbcSchema schema, List<QColumn> fkColumns, QJoin associateJoin) {
        QJoin join = new QJoin(schema, fkColumns, associateJoin);
        QJoin old = super.get(join.getJsonKey());
        if (old != null) {
            join.resolveNameConflict(old);
        }
        old = this.put(join.getJsonKey(), join);
        assert (old == null);
    }

    static class Builder extends HashMap<QSchema, QJoin> {
        private String tableName;
        private HashSet<QSchema> conflictMappings = new HashSet<>();
        public Builder(JdbcSchema pkSchema) {
            this.tableName = pkSchema.generateEntityClassName().toLowerCase();
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
            for (QSchema schema : conflictMappings) {
                super.remove(schema);
            }

            JoinMap joinMap = new JoinMap();
            joinMap.putAll(baseSchema.getImportedJoins());
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
}
