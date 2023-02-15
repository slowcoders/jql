package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.jpa.JPARepositoryBase;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.SchemaLoader;
import org.eipgrid.jql.jdbc.metadata.JdbcSchemaLoader;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.util.CaseConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class JdbcStorage extends JqlStorage {
    JdbcSchemaLoader jdbcSchemaLoader;
    private final JdbcTemplate jdbc;
    private HashMap<String, JqlRepository> repositories = new HashMap<>();
    private HashMap<Class, JPARepositoryBase> jpaRepositories = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) throws Exception {
        super(transactionTemplate, conversionService,
                entityManager);
        this.jdbc = new JdbcTemplate(dataSource);
        jdbcSchemaLoader = new JdbcSchemaLoader(entityManager, dataSource, CaseConverter.defaultConverter);
    }

    public SchemaLoader getSchemaLoader() {
        return jdbcSchemaLoader;
    }

    public DataSource getDataSource() {
        return this.jdbc.getDataSource();
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbc;
    }

    private JqlRepository createRepository(QSchema schema) {
        JqlRepository repo;
        synchronized (repositories) {
            String tableName = schema.getTableName();
            if (schema.isJPARequired()) {
                Class<?> entityType = schema.getEntityType();
                repo = new JPARepositoryBase(this, entityType);
                jpaRepositories.put(entityType, (JPARepositoryBase)repo);
            } else {
                repo = new JDBCRepositoryBase(this, schema);
            }
            repositories.put(tableName, repo);
        }
        return repo;
    }

    public JqlRepository getRepository(String tableName) {
        JqlRepository repo = repositories.get(tableName);
        if (repo == null) {
            synchronized (repositories) {
                repo = repositories.get(tableName);
                if (repo == null) {
                    QSchema schema = jdbcSchemaLoader.loadSchema(tableName);
                    repo = createRepository(schema);
                }
            }
        }
        return repo;
    }

    public <T,ID> JPARepositoryBase<T,ID> getRepository(Class<T> entityType) {
        JPARepositoryBase repo = jpaRepositories.get(entityType);
        if (repo == null) {
            synchronized (repositories) {
                repo = jpaRepositories.get(entityType);
                if (repo == null) {
                    QSchema schema = jdbcSchemaLoader.loadSchema(entityType);
                    repo = (JPARepositoryBase)createRepository(schema);
                }
            }
        }
        return repo;
    }

    public QSchema loadSchema(String tablePath) {
        return jdbcSchemaLoader.loadSchema(tablePath);
    }

    public QSchema loadSchema(Class entityType) {
        return jdbcSchemaLoader.loadSchema(entityType);
    }

    public List<String> getTableNames(String dbSchema) throws SQLException {
        return jdbcSchemaLoader.getTableNames(dbSchema);
    }

    public List<String> getNamespaces() {
        return jdbcSchemaLoader.getNamespaces();
    }

    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return jdbcSchemaLoader.createSqlGenerator(isNativeQuery);
    }
}
