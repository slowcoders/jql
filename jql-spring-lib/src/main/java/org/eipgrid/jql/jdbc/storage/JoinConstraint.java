package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

import java.util.ArrayList;

public class JoinConstraint extends ArrayList<QColumn> {
    private final String constraintName;

    private final JdbcSchema fkSchema;
    private JdbcSchema pkSchema;

    JoinConstraint(JdbcSchema fkSchema, String constraintName) {
        this.constraintName = constraintName;
        this.fkSchema = fkSchema;
    }

    public String getConstraintName() {
        return this.constraintName;
    }

    public JdbcSchema getFkSchema() {
        return fkSchema;
    }

    public JdbcSchema resolvePkSchema() {
        if (pkSchema == null) {
            pkSchema = (JdbcSchema) super.get(0).getJoinedPrimaryColumn().getSchema();
            for (QColumn column : this) {
                assert (column.getJoinedPrimaryColumn().getSchema() == pkSchema);
            }
        }
        return pkSchema;
    }

    public JdbcSchema getPeerSchema(QSchema schema) {
        if (schema == resolvePkSchema()) {
            return fkSchema;
        }
        else if (schema == fkSchema) {
            return pkSchema;
        }
        else {
            throw new RuntimeException("invalid schema " + schema);
        }
    }

    @Override
    public boolean add(QColumn column) {
        assert(column.getSchema() == fkSchema);
        return super.add(column);
    }

    public QColumn getColumnByPhysicalName(String name) {
        for (QColumn col : this) {
            if (col.getPhysicalName().equalsIgnoreCase(name)) {
                return col;
            }
        }
        return null;
    }
}
