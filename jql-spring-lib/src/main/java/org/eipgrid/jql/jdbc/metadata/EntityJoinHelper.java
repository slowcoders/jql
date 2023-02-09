package org.eipgrid.jql.jdbc.metadata;

import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QJoin;

import java.util.HashMap;

class EntityJoinHelper extends HashMap<QSchema, QJoin> {
    private String tableName;
    public EntityJoinHelper(QSchema pkSchema) {
        this.tableName = pkSchema.getSimpleTableName();
    }

    public QJoin put(QSchema schema, QJoin childJoin) {
        QJoin oldJoin = super.put(schema, childJoin);
        if (oldJoin != null && oldJoin != childJoin) {
            if (oldJoin.getJsonKey().startsWith(tableName)) {
                super.put(schema, oldJoin);
                return oldJoin;
            } else if (childJoin.getJsonKey().equals(tableName)) {
                // do nothing;
                return oldJoin;
            }
            throw new RuntimeException("Conflict Mapped Schema");
        }
        return oldJoin;
    }
}
