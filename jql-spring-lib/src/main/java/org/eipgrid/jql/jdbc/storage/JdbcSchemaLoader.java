package org.eipgrid.jql.jdbc.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public abstract class JdbcSchemaLoader extends JqlStorage {
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbc;
    private String defaultNamespace;
    private boolean schemaSupported;

    private String dbType;
    private HashMap<String, Class<?>> ormTypeMap;

    private final HashMap<Class<?>, JdbcSchema> classToSchemaMap = new HashMap<>();
    private final HashMap<String, JdbcSchema> schemaMap = new HashMap<>();


    protected JdbcSchemaLoader(DataSource dataSource, TransactionTemplate transactionTemplate, ObjectMapper objectMapper, EntityManager entityManager) {
        super(transactionTemplate, objectMapper);
        this.jdbc = new JdbcTemplate(dataSource);
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        Properties dbProperties = jdbc.execute(new ConnectionCallback<Properties>() {
            @Override
            public Properties doInConnection(Connection conn) throws SQLException, DataAccessException {
                dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
                String schema = conn.getSchema();
                schemaSupported = schema != null;
                if (schemaSupported) {
                    defaultNamespace = schema;
                } else {
                    defaultNamespace = conn.getCatalog();
                }
                return null;
            }
        });
    }

    public final EntityManager getEntityManager() { return entityManager; }

    public final String getDbType() { return dbType; }

    public final DataSource getDataSource() {
        return this.jdbc.getDataSource();
    }

    public final JdbcTemplate getJdbcTemplate() {
        return jdbc;
    }

    public final boolean isDBSchemaSupported() {
        return this.schemaSupported;
    }
    
    private void initialize() {
        if (ormTypeMap != null) return;
        synchronized (this) {
            if (ormTypeMap != null) return;
            ormTypeMap = new HashMap<>();

            Set<EntityType<?>> types = entityManager.getEntityManagerFactory().getMetamodel().getEntities();
            for (EntityType<?> type : types) {
                Class<?> clazz = type.getJavaType();
                TablePath tablePath = TablePath.of(clazz, this);
                if (tablePath != null) {
                    ormTypeMap.put(tablePath.getQualifiedName(), clazz);
                }
            }
        }
    }


    public String getDefaultNamespace() { return this.defaultNamespace; }


    public QSchema loadSchema(Class entityType) {
        initialize();
        QSchema schema = classToSchemaMap.get(entityType);
        if (schema == null) {
            TablePath tablePath = TablePath.of(entityType, this);
            schema = loadSchema(tablePath, entityType);
        }
        return schema;
    }

    public QSchema loadSchema(String tableName) {
        initialize();
        QSchema schema = schemaMap.get(tableName);
        if (schema == null) {
            Class<?> ormType = ormTypeMap.get(tableName);
            if (ormType == null) {
                ormType = JqlRepository.RawEntityType;
            }
            TablePath tablePath = TablePath.of(tableName, this);
            schema = loadSchema(tablePath, ormType);
        }
        return schema;
    }

    private QSchema loadSchema(TablePath tablePath, Class<?> ormType0) {

        final Class<?> ormType = ormType0;
        synchronized (schemaMap) {
            QSchema schema = jdbc.execute(new ConnectionCallback<QSchema>() {
                @Override
                public QSchema doInConnection(Connection conn) throws SQLException, DataAccessException {
                    return loadSchema(conn, tablePath, ormType);
                }
            });
            return schema;
        }
    }

    private QSchema loadSchema(Connection conn, TablePath tablePath, Class<?> ormType) throws SQLException {
        JdbcSchema schema = schemaMap.get(tablePath.getQualifiedName());
        if (schema != null) return schema;

        String qname = tablePath.getQualifiedName();
        schema = new JdbcSchema(JdbcSchemaLoader.this, qname, ormType);

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
        schemaMap.put(qname, schema);
        if (schema.isJPARequired()) {
            classToSchemaMap.put(ormType, schema);
        }
        return schema;
    }

    protected void loadJoinMap(QSchema schema) {
        synchronized (schemaMap) {
            jdbc.execute(new ConnectionCallback<Void>() {
                @Override
                public Void doInConnection(Connection conn) throws SQLException, DataAccessException {
                    loadExternalJoins(conn, (JdbcSchema) schema);
                    return null;
                }
            });
        }
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

    public List<String> getTableNames(String namespace) {
        List<String> tableNames = jdbc.execute(new ConnectionCallback<List<String>>() {
            @Override
            public List<String> doInConnection(Connection conn) throws SQLException, DataAccessException {
                return getTableNames(conn, namespace);
            }
        });
        return tableNames;
    }

    public List<String> getNamespaces() {
        List<String> namespaces = jdbc.execute(new ConnectionCallback<List<String>>() {
            @Override
            public List<String> doInConnection(Connection conn) throws SQLException, DataAccessException {
                DatabaseMetaData md = conn.getMetaData();
                ResultSet rs = schemaSupported ? md.getSchemas() : md.getCatalogs();
                ArrayList<String> names = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString(schemaSupported ? "TABLE_SCHEM": "TABLE_CAT");
                    names.add(name);
                }
                return names;
            }
        });
        return namespaces;
    }


    private ArrayList<String> getTableNames(Connection conn, String namespace) throws SQLException {
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
            assert(table_cat == null);
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
            fk.bindPrimaryKey(new ColumnBinder(this, join.pkTableQName, join.pkColumnName));
            fkSchema.addForeignKeyConstraint(join.fk_name, fk);
        }
    }

    private void loadExternalJoins(Connection conn, JdbcSchema pkSchema) throws SQLException {
        Map<String, QJoin> res = pkSchema.getEntityJoinMap(false);
        if (res != null) {
            return;
        }

        final TablePath tablePath = TablePath.of(pkSchema.getTableName(), JdbcSchemaLoader.this);
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getExportedKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());

        JoinMap.Builder joins = new JoinMap.Builder(pkSchema);
        while (rs.next()) {
            JoinData join = new JoinData(rs, this);
            JdbcSchema fkSchema = (JdbcSchema) loadSchema(join.fkTableQName);
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

    public String getTableComment(String tableName) {
        TablePath tablePath = TablePath.of(tableName, this);
        String comment = null;
        if ("postgresql".equals(dbType)) {
        }
        else if ("mariadb".equals(dbType) || "mysql".equals(dbType)) {
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
        }
        return comment;
    }


    private Map<String, String> getColumnComments(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, String> comments = new HashMap<>();
        if ("postgresql".equals(dbType)) {
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
        }
        else if ("mariadb".equals(dbType) || "mysql".equals(dbType)) {
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
        }
        return comments;
    }

    private void dumResultSet(ResultSet rs) throws SQLException {
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

    static class JoinData {
        String pk_name;
        String pktable_schem;
        String pktable_name;

        // ??????) fk_name ??? column-name ??? ?????????, fk constraint ??? name ??????.
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

            // ??????) fk_name ??? column-name ??? ?????????, fk constraint ??? name ??????.
            this.fk_name = rs.getString("fk_name");
            this.fktable_schem = rs.getString("fktable_schem");
            this.fktable_name  = rs.getString("fktable_name");
            this.fktable_cat = rs.getString("fktable_cat");

            this.pkColumnName = rs.getString("pkcolumn_name");
            this.fkColumnName = rs.getString("fkcolumn_name");
            this.fkTableQName = TablePath.of(fktable_cat, fktable_schem, fktable_name, loader).getQualifiedName();
            this.pkTableQName = TablePath.of(pktable_cat, pktable_schem, pktable_name, loader).getQualifiedName();

            this.key_seq = rs.getInt("key_seq");
            this.update_rule = rs.getInt("update_rule");
            this.delete_rule = rs.getInt("delete_rule");
            this.deferrability = rs.getInt("deferrability");
        }
    }

    protected SqlGenerator createSqlGenerator(boolean isNativeQuery) {
        return new SqlGenerator(isNativeQuery);
    }
}
