package org.eipgrid.jql.jdbc.metadata;

import org.eipgrid.jql.schema.QColumn;

class ColumnBinder {
    private final JdbcSchemaLoader schemaLoader;
    private final String tableName;
    private final String columnName;
    private QColumn pk;

    ColumnBinder(JdbcSchemaLoader schemaLoader, String tableName, String columnName) {
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
