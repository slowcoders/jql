package org.eipgrid.jql.jdbc.mysql;

import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;
import org.eipgrid.jql.jdbc.storage.SqlGenerator;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySqlSchemaLoader extends JdbcSchemaLoader {


    private final JdbcTemplate jdbc;

    public MySqlSchemaLoader(JdbcStorage storage, Connection conn) throws SQLException {
        super(storage, conn.getCatalog(), false);
        this.jdbc = new JdbcTemplate(storage.getDataSource());
    }


    public String getTableComment(String tableName) {
        TablePath tablePath = TablePath.of(tableName);
        String comment = null;
        String sql = "SELECT table_name, table_comment\n" +
                "FROM information_schema.tables\n" +
                "WHERE table_schema = '" + tablePath.getCatalog() + "' and table_name = '" + tablePath.getSimpleName() + "'";

        List<Map<String, Object>> rs = jdbc.queryForList(sql);
        for (Map<String, Object> row : rs) {
            comment = (String) row.get("table_comment");
            if (comment != null) {
                comment = comment.trim();
            }
        }
        return comment;
    }


    private Map<String, String> getColumnComments(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, String> comments = new HashMap<>();
        String sql = "SELECT table_name, column_name, column_comment\n" +
                "FROM information_schema.columns\n" +
                "WHERE table_schema = '" + tablePath.getCatalog() + "' and table_name = '" + tablePath.getSimpleName() + "'";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            String comment = rs.getString("column_comment");
            if (comment != null && comment.trim().length() > 0) {
                String columnName = rs.getString("column_name");
                comments.put(columnName, comment);
            }
        }
        return comments;
    }


    public String createDDL(QSchema schema) {
//        SQLWriter sb = new SQLWriter(schema);
//        sb.write("const " + schema.getTableName() + "Schema = [\n");
//        for (JQColumn col : schema.getColumns()) {
//            sb.write(col.dumpJSONSchema()).write(",\n");
//        }
//        sb.write("]\n");
//        return sb.toString();
        throw new RuntimeException("not implemented");
    }

    public SqlGenerator createSqlGenerator(boolean isNativeQuery) {
        return new MySqlGenerator(isNativeQuery);
    }
}
