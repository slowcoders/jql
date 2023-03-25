package org.eipgrid.jql.jdbc.postgresql;

import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;

import java.sql.Connection;
import java.sql.SQLException;

public class SchemaLoaderFactory implements org.eipgrid.jql.jdbc.storage.SchemaLoaderFactory {
    @Override
    public JdbcSchemaLoader createSchemaLoader(JdbcStorage storage, Connection conn) throws SQLException {
        return new PGSchemaLoader(storage, conn);
    }
}
