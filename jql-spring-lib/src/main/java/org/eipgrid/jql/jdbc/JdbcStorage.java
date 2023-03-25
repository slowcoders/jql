package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.jdbc.storage.*;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.jpa.JqlAdapter;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.ClassUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class JdbcStorage extends JqlStorage {

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbc;
    private HashMap<String, Class<?>> ormTypeMap;

    private final HashMap<Class<?>, JdbcSchema> classToSchemaMap = new HashMap<>();
    private final HashMap<String, JdbcSchema> schemaMap = new HashMap<>();

    private final JdbcSchemaLoader jdbcSchemaLoader;

    private String dbType;
    private HashMap<String, JdbcRepositoryBase> repositories = new HashMap<>();
    private static HashMap<Class, JpaTable> jpaTables = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) {
        super(transactionTemplate, objectMapper);
        this.jdbc = new JdbcTemplate(dataSource);
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        this.jdbcSchemaLoader = jdbc.execute(new ConnectionCallback<JdbcSchemaLoader>() {
            @Override
            public JdbcSchemaLoader doInConnection(Connection conn) throws SQLException, DataAccessException {
                dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
                String factoryName = JdbcStorage.class.getPackageName() + '.' + dbType + '.' + "SchemaLoaderFactory";
                SchemaLoaderFactory factory = ClassUtils.newInstanceOrNull(factoryName);
                return factory.createSchemaLoader(JdbcStorage.this, conn);
            }
        });
    }

    public String getDbType() {
        return this.dbType;
    }

    public final EntityManager getEntityManager() { return entityManager; }

    public final DataSource getDataSource() {
        return this.jdbc.getDataSource();
    }

    public final JdbcTemplate getJdbcTemplate() {
        return jdbc;
    }


    public JdbcRepositoryBase registerTable(JqlAdapter table, Class entityType) {
        synchronized (jpaTables) {
            if (jpaTables.put(entityType, (JpaTable) table) != null) {
                throw new RuntimeException("Jpa repository already registered " + entityType.getName());
            }
            QSchema schema = this.loadSchema(entityType);
            JdbcRepositoryBase repo = new JdbcRepositoryImpl(this, schema);
            return repo;
        }
    }

    protected void registerTable(JdbcRepositoryBase table) {

        synchronized (jpaTables) {
            QSchema schema = table.getSchema();
            JqlRepository old_table;

            String tableName = table.getTableName();
            old_table = repositories.put(tableName, table);
            if (old_table != null) {
                throw new Error("Duplicated JdbcTable on the table " + table.getTableName());
            }
        }
    }


    private JdbcRepositoryBase createRepository(QSchema schema) {
        JdbcRepositoryBase repo;
        synchronized (jpaTables) {
            if (schema.isJPARequired()) {
                Class<?> entityType = schema.getEntityType();
                JpaTableImpl table = new JpaTableImpl(this, entityType);
                repo = repositories.get(schema.getTableName());
            } else {
                repo = new JdbcRepositoryImpl(this, schema);
            }
        }
        return repo;
    }

    public JqlEntitySet loadEntitySet(String tableName) {
        JdbcRepositoryBase repo = loadRepository(tableName);
        JqlEntitySet table = jpaTables.get(repo.getSchema().getEntityType());
        if (table == null) table = repo;
        return table;
    }

    public JdbcRepositoryBase loadRepository(String tableName) {
        JdbcRepositoryBase repo = repositories.get(tableName);
        if (repo == null) {
            synchronized (jpaTables) {
                repo = repositories.get(tableName);
                if (repo == null) {
                    QSchema schema = this.loadSchema(tableName);
                    repo = createRepository(schema);
                }
            }
        }
        return repo;
    }

    public <T,ID> JpaTable<T,ID> loadJpaTable(Class<T> entityType) {
        JpaTable table = jpaTables.get(entityType);
        if (table == null) {
            QSchema schema = this.loadSchema(entityType);
            synchronized (jpaTables) {
                table = jpaTables.get(entityType);
                if (table == null) {
                    table = new JpaTableImpl<>(this, entityType);
                }
            }
        }
        return table;
    }

    public static <T,ID> JpaTable<T,ID> findJpaTable(Class<T> entityType) {
        JpaTable table = jpaTables.get(entityType);
        return table;
    }

    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }


    private class JpaTableImpl<ENTITY, ID> extends JpaTable<ENTITY, ID> {
        private final PersistenceUnitUtil persistenceUnitUtil;

        public JpaTableImpl(JdbcStorage storage, Class<ENTITY> entityType) {
            super(storage, entityType);
            persistenceUnitUtil = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        }
        public ID getEntityId(ENTITY entity) {
            ID id = (ID)persistenceUnitUtil.getIdentifier(entity);
            return id;
        }
    }

    private void initialize() {
        if (ormTypeMap != null) return;
        synchronized (this) {
            if (ormTypeMap != null) return;
            ormTypeMap = new HashMap<>();

            Set<EntityType<?>> types = entityManager.getEntityManagerFactory().getMetamodel().getEntities();
            for (EntityType<?> type : types) {
                Class<?> clazz = type.getJavaType();
                JdbcSchemaLoader.TablePath tablePath = jdbcSchemaLoader.getTablePath(clazz);
                if (tablePath != null) {
                    ormTypeMap.put(tablePath.getQualifiedName(), clazz);
                }
            }
        }
    }


    public QSchema loadSchema(Class entityType) {
        initialize();
        QSchema schema = classToSchemaMap.get(entityType);
        if (schema == null) {
            JdbcSchemaLoader.TablePath tablePath = jdbcSchemaLoader.getTablePath(entityType);
            schema = loadSchema(tablePath, entityType);
        }
        return schema;
    }

    public JdbcSchema loadSchema(String tableName) {
        initialize();
        JdbcSchema schema = schemaMap.get(tableName);
        if (schema == null) {
            Class<?> ormType = ormTypeMap.get(tableName);
            if (ormType == null) {
                ormType = JqlRepository.rawEntityType;
            }
            JdbcSchemaLoader.TablePath tablePath = JdbcSchemaLoader.TablePath.of(tableName);
            schema = loadSchema(tablePath, ormType);
        }
        return schema;
    }

    private JdbcSchema loadSchema(JdbcSchemaLoader.TablePath tablePath, Class<?> ormType0) {

        final Class<?> ormType = ormType0;
        synchronized (schemaMap) {
            JdbcSchema schema = jdbc.execute(new ConnectionCallback<JdbcSchema>() {
                @Override
                public JdbcSchema doInConnection(Connection conn) throws SQLException, DataAccessException {
                    JdbcSchema schema = schemaMap.get(tablePath.getQualifiedName());
                    if (schema == null) {
                        schema = jdbcSchemaLoader.loadSchema(conn, tablePath, ormType);
                        schemaMap.put(tablePath.getQualifiedName(), schema);
                        if (schema.isJPARequired()) {
                            classToSchemaMap.put(ormType, schema);
                        }
                    }
                    return schema;
                }
            });
            return schema;
        }
    }


    public void loadJoinMap(QSchema schema) {
        synchronized (schemaMap) {
            jdbc.execute(new ConnectionCallback<Void>() {
                @Override
                public Void doInConnection(Connection conn) throws SQLException, DataAccessException {
                    jdbcSchemaLoader.loadExternalJoins(conn, (JdbcSchema) schema);
                    return null;
                }
            });
        }
    }

    public List<String> getTableNames(String namespace) {
        List<String> tableNames = jdbc.execute(new ConnectionCallback<List<String>>() {
            @Override
            public List<String> doInConnection(Connection conn) throws SQLException, DataAccessException {
                return jdbcSchemaLoader.getTableNames(conn, namespace);
            }
        });
        return tableNames;
    }

    @Override
    public List<String> getNamespaces() {
        List<String> namespaces = jdbc.execute(new ConnectionCallback<List<String>>() {
            @Override
            public List<String> doInConnection(Connection conn) throws SQLException, DataAccessException {
                return jdbcSchemaLoader.getNamespaces(conn);
            }
        });
        return namespaces;
    }


    protected SqlGenerator createQueryGenerator(boolean isNativeQuery) {
        return jdbcSchemaLoader.createSqlGenerator(isNativeQuery);
    }

    public String getTableComment(String tableName) {
        return jdbcSchemaLoader.getTableComment(tableName);
    }
}
