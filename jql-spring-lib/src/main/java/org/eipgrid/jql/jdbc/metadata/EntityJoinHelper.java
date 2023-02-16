package org.eipgrid.jql.jdbc.metadata;

import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QJoin;

import java.util.HashMap;
import java.util.HashSet;

class EntityJoinHelper extends HashMap<QSchema, QJoin> {
    private String tableName;
    private HashSet<QSchema> conflictMappings = new HashSet<>();
    public EntityJoinHelper(JdbcSchema pkSchema) {
        this.tableName = pkSchema.suggestEntityClassName().toLowerCase();
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
}
