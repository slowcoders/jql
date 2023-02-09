package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.SchemaLoader;
import org.eipgrid.jql.jdbc.metadata.JdbcSchemaLoader;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.util.CaseConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class JdbcStorage extends JqlStorage {
    JdbcSchemaLoader jdbcSchemaLoader;
    private HashMap<String, JqlRepository> repositories = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ConversionService conversionService,
                       EntityManager entityManager) throws Exception {
        super(dataSource, transactionTemplate, conversionService,
                entityManager);
        jdbcSchemaLoader = new JdbcSchemaLoader(entityManager, dataSource, CaseConverter.defaultConverter);
    }

    public SchemaLoader getSchemaLoader() {
        return jdbcSchemaLoader;
    }

    public JqlRepository getRepository(String tableName) {
        JqlRepository repo = repositories.get(tableName);
        if (repo == null) {
            // TODO ormType
            QSchema schema = jdbcSchemaLoader.loadSchema(tableName);
            repo = new JDBCRepositoryBase(this, schema);
            repositories.put(tableName, repo);
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

    public List<String> getDBSchemas() {
        return jdbcSchemaLoader.getDBSchemas();
    }

    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return jdbcSchemaLoader.createSqlGenerator(isNativeQuery);
    }
}
