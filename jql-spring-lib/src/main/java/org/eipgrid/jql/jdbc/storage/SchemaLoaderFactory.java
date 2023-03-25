package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.jdbc.JdbcStorage;

import java.sql.Connection;
import java.sql.SQLException;

public interface SchemaLoaderFactory {
    JdbcSchemaLoader createSchemaLoader(JdbcStorage storage, Connection conn) throws SQLException;
}
