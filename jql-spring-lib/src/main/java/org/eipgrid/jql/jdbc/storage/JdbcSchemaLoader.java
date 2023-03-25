package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.dao.DataAccessException;

import javax.persistence.Table;
import java.sql.*;
import java.util.*;

public abstract class JdbcSchemaLoader {

    protected final JdbcStorage storage;

    protected final String defaultNamespace;

    protected final boolean schemaSupported;

    public JdbcSchemaLoader(JdbcStorage storage, String defaultNamespace, boolean schemaSupported) {
        this.storage = storage;
        this.schemaSupported = schemaSupported;
        if (defaultNamespace != null && defaultNamespace.trim().length() == 0) {
            defaultNamespace = null;
        }
        this.defaultNamespace = defaultNamespace;
    }

    public abstract SqlGenerator createSqlGenerator(boolean isNativeQuery);

    public JdbcSchema loadSchema(Connection conn, TablePath tablePath, Class<?> ormType) throws SQLException {
        String qname = tablePath.getQualifiedName();
        JdbcSchema schema = new JdbcSchema(storage, qname, ormType);

        ArrayList<String> primaryKeys = getPrimaryKeys(conn, tablePath);
        HashMap<String, ArrayList<String>> uniqueConstraints = getUniqueConstraints(conn, tablePath);
        if (primaryKeys.size() == 0) {
            for (ArrayList<String> keys : uniqueConstraints.values()) {
                if (primaryKeys.size() == 0 || keys.size() < primaryKeys.size()) {
                    primaryKeys = keys;
                }
            }
        }
        ArrayList<QColumn> columns = getColumns(conn, tablePath, schema, primaryKeys);
        processForeignKeyConstraints(conn, schema, tablePath, columns);
        schema.init(columns, uniqueConstraints, ormType);
        return schema;
    }

    private ArrayList<String> getPrimaryKeys(Connection conn, TablePath tablePath) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getPrimaryKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
        ArrayList<String> keys = new ArrayList<>();
        int next_key_seq = 1;
        while (rs.next()) {
            String key = rs.getString("column_name");
            int seq = rs.getInt("key_seq");
            if (seq != next_key_seq) {
                throw new RuntimeException("something wrong");
            }
            next_key_seq ++;
            keys.add(key);
        }
        return keys;
    }

    public List<String> getNamespaces(Connection conn) throws SQLException, DataAccessException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = schemaSupported ? md.getSchemas() : md.getCatalogs();
        ArrayList<String> names = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString(schemaSupported ? "TABLE_SCHEM": "TABLE_CAT");
            names.add(name);
        }
        return names;
    }


    public ArrayList<String> getTableNames(Connection conn, String namespace) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = md.getTables(namespace, namespace, "%", types);
        ArrayList<String> names = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            names.add(name);
        }
        return names;
    }

    private HashMap<String, ArrayList<String>> getUniqueConstraints(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, ArrayList<String>> indexMap = new HashMap<>();

        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getIndexInfo(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName(), true, false);
        while (rs.next()) {
            String table_schem = rs.getString("table_schem");
            String table_name = rs.getString("table_name");
            String index_qualifier = rs.getString("index_qualifier");
            String index_name = rs.getString("index_name");
            String column_name = rs.getString("column_name");
            String filter_condition = rs.getString("filter_condition");
            boolean is_unique = !rs.getBoolean("non_unique");
            String sort = rs.getString("asc_or_desc");
            int type = rs.getInt("type");
            int ordinal_position = rs.getInt("ordinal_position");
            int cardinality = rs.getInt("cardinality");
            int pages = rs.getInt("pages");

            String table_cat = rs.getString("table_cat");
            assert(is_unique);

            ArrayList<String> indexes = indexMap.get(index_name);
            if (indexes == null) {
                indexes = new ArrayList<>();
                indexMap.put(index_name, indexes);
            }
            indexes.add(column_name);
        }
        return indexMap;
    }

    private JdbcColumn getColumnByPhysicalName(ArrayList<QColumn> columns, String columnName) {
        columnName = columnName.toLowerCase();
        for (QColumn column : columns) {
            if (columnName.equals(column.getPhysicalName().toLowerCase())) {
                return (JdbcColumn)column;
            }
        }
        throw new RuntimeException("column not found: " + columnName);
    }

    private void processForeignKeyConstraints(Connection conn, JdbcSchema fkSchema, TablePath tablePath, ArrayList<QColumn> columns) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getImportedKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
        while (rs.next()) {
            JoinData join = new JoinData(rs, this);
            JdbcColumn fk = getColumnByPhysicalName(columns, join.fkColumnName);
            fk.bindPrimaryKey(new ColumnBinder(storage, join.pkTableQName, join.pkColumnName));
            fkSchema.addForeignKeyConstraint(join.fk_name, fk);
        }
    }

    public void loadExternalJoins(Connection conn, JdbcSchema pkSchema) throws SQLException {
        Map<String, QJoin> res = pkSchema.getEntityJoinMap(false);
        if (res != null) {
            return;
        }

        final TablePath tablePath = TablePath.of(pkSchema.getTableName());
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getExportedKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());

        JoinMap.Builder joins = new JoinMap.Builder(pkSchema);
        while (rs.next()) {
            JoinData join = new JoinData(rs, this);
            JdbcSchema fkSchema = (JdbcSchema)storage.loadSchema(join.fkTableQName);
            QJoin fkJoin = fkSchema.getJoinByForeignKeyConstraints(join.fk_name);
            joins.put(fkSchema, fkJoin);
        }
        Map<String, QJoin> joinMap = joins.createJoinMap((JdbcSchema) pkSchema);
        pkSchema.setEntityJoinMap(joinMap);
    }

    private ArrayList<QColumn> getColumns(Connection conn, TablePath tablePath, JdbcSchema schema, ArrayList<String> primaryKeys) throws SQLException {
        //HashMap<String, JqlIndex> indexes = getUniqueConstraints(conn, dbSchema, tableName);
        Map<String, String> comments = getColumnComments(conn, tablePath);
        ArrayList<QColumn> columns = new ArrayList<>();
        String qname = tablePath.getQualifiedName();
        ResultSet rs = conn.createStatement().executeQuery("select * from " + qname + " limit 1");
        ResultSetMetaData md = rs.getMetaData();
        int cntColumn = md.getColumnCount();
        for (int col = 0; ++col <= cntColumn; ) {
            String columnName = md.getColumnName(col);
            //ColumnBinder joinedPK = joinedPKs.get(columnName);
            String comment = comments.get(columnName);
            JdbcColumn ci = new JdbcColumn(schema, md, col, null, comment, primaryKeys);
            columns.add(ci);
        }
        return columns;
    }

    public abstract String getTableComment(String tableName);


    private Map<String, String> getColumnComments(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, String> comments = new HashMap<>();
        String sql = "SELECT c.column_name, pgd.description\n" +
                "FROM information_schema.columns c\n" +
                "    inner join pg_catalog.pg_statio_all_tables as st on (c.table_name = st.relname)\n" +
                "    inner join pg_catalog.pg_description pgd on (pgd.objoid=st.relid and\n" +
                "          pgd.objsubid=c.ordinal_position)\n" +
                "where c.table_schema = '" + tablePath.getSchema() + "' and c.table_name = '" + tablePath.getSimpleName() + "';\n";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            String columnName = rs.getString("column_name");
            String comment = rs.getString("description");
            comments.put(columnName, comment);
        }
        return comments;
    }

    public void dumResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        while (rs.next()) {
            for (int i = 0; ++i <= colCount; ) {
                String colName = meta.getColumnName(i);
                String value = rs.getString(i);
                System.out.println(colName + ": " + value);
            }
        }
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

    public TablePath getTablePath(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        return table != null ? TablePath.of(table, this.schemaSupported) : null;
    }

    static class JoinData {
        String pk_name;
        String pktable_schem;
        String pktable_name;

        // 참고) fk_name 은 column-name 이 아니라, fk constraint 의 name 이다.
        String fk_name;
        String fktable_schem;
        String fktable_name;

        String pkColumnName;
        String fkColumnName;
        String fkTableQName;
        String pkTableQName;

        int key_seq;
        int update_rule;
        int delete_rule;
        int deferrability;
        String pktable_cat;
        String fktable_cat;

        JoinData(ResultSet rs, JdbcSchemaLoader loader) throws SQLException {
            this.pk_name = rs.getString("pk_name");
            this.pktable_schem = rs.getString("pktable_schem");
            this.pktable_name  = rs.getString("pktable_name");
            this.pktable_cat = rs.getString("pktable_cat");

            // 참고) fk_name 은 column-name 이 아니라, fk constraint 의 name 이다.
            this.fk_name = rs.getString("fk_name");
            this.fktable_schem = rs.getString("fktable_schem");
            this.fktable_name  = rs.getString("fktable_name");
            this.fktable_cat = rs.getString("fktable_cat");

            this.pkColumnName = rs.getString("pkcolumn_name");
            this.fkColumnName = rs.getString("fkcolumn_name");
            this.fkTableQName = loader.makeQualifiedName(fktable_cat, fktable_schem, fktable_name);
            this.pkTableQName = loader.makeQualifiedName(pktable_cat, pktable_schem, pktable_name);

            this.key_seq = rs.getInt("key_seq");
            this.update_rule = rs.getInt("update_rule");
            this.delete_rule = rs.getInt("delete_rule");
            this.deferrability = rs.getInt("deferrability");
        }
    }

    private String makeQualifiedName(String db_category, String db_schema, String table_name) {
        String namespace = schemaSupported ? db_schema : db_category;
        if (namespace == null || (namespace = namespace.trim()).length() == 0) {
            namespace = defaultNamespace;
        }
        return namespace == null ? table_name : namespace + '.' + table_name;
    }

    public static class TablePath {
        private final String catalog;
        private final String schema;
        private final String qualifiedName;
        private final String simpleName;

        TablePath(String catalog, String schema, String qualifiedName, String simpleName) {
            this.catalog = catalog;
            this.schema = schema;
            this.qualifiedName = qualifiedName;
            this.simpleName = simpleName;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }


        public static TablePath of(Class<?> clazz, boolean useSchema) {
            Table table = clazz.getAnnotation(Table.class);
            return table != null ? of(table, useSchema) : null;
        }

        public static TablePath of(Table table, boolean useSchema) {
            String name = table.name();
            String namespace = useSchema ? table.schema() : table.catalog();
            return of(namespace, name);
        }

        public static TablePath of(String namespace, String simpleName) {
            namespace = namespace == null ? "" : namespace.trim();
            simpleName = simpleName.trim();

            String qualifiedName = namespace.length() > 0 ? namespace + '.' + simpleName : simpleName;
            return new TablePath(namespace, namespace, qualifiedName, simpleName);
        }

        public static TablePath of(String qualifiedName) {
            qualifiedName = qualifiedName.toLowerCase();
            int last_dot_p = qualifiedName.lastIndexOf('.');
            String namespace = last_dot_p > 0 ? qualifiedName.substring(0, last_dot_p) : null;//getDefaultDBSchema();
            String simpleName = qualifiedName.substring(last_dot_p + 1);
            return new TablePath(namespace, namespace, qualifiedName, simpleName);
        }
    }
}
