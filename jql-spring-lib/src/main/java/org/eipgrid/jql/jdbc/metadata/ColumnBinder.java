package org.eipgrid.jql.jdbc.metadata;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.SchemaLoader;

class ColumnBinder {
    private final SchemaLoader schemaLoader;
    private final String tableName;
    private final String columnName;
    private QColumn pk;

    ColumnBinder(SchemaLoader schemaLoader, String tableName, String columnName) {
        this.schemaLoader = schemaLoader;
        this.tableName = tableName;
        this.columnName = columnName.toLowerCase();
    }

    public QColumn getJoinedColumn() {
        if (pk == null) {
            pk = schemaLoader.loadSchema(tableName).getColumn(columnName);
            assert (pk != null);
        }
        return pk;
    }
}
