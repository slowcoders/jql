package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;
import org.eipgrid.jql.jdbc.storage.QueryGenerator;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.HashMap;

public class JdbcStorage extends JdbcSchemaLoader {
    private HashMap<String, JdbcTable> repositories = new HashMap<>();
    private static HashMap<Class, JpaTable> jpaRepositories = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) {
        super(dataSource, transactionTemplate, objectMapper, entityManager);
    }

    private JdbcTable createRepository(QSchema schema) {
        JdbcTable repo;
        synchronized (repositories) {
            String tableName = schema.getTableName();
            if (schema.isJPARequired()) {
                Class<?> entityType = schema.getEntityType();
                repo = new JpaTable(this, entityType);
                jpaRepositories.put(entityType, (JpaTable)repo);
            } else {
                repo = new JdbcTable(this, schema);
            }
            repositories.put(tableName, repo);
        }
        return repo;
    }

    public JdbcTable getRepository(String tableName) {
        JdbcTable repo = repositories.get(tableName);
        if (repo == null) {
            synchronized (repositories) {
                repo = repositories.get(tableName);
                if (repo == null) {
                    QSchema schema = super.loadSchema(tableName);
                    repo = createRepository(schema);
                }
            }
        }
        return repo;
    }

    public <T,ID> JpaTable<T,ID> getRepository(Class<T> entityType) {
        JpaTable repo = jpaRepositories.get(entityType);
        if (repo == null) {
            synchronized (repositories) {
                repo = jpaRepositories.get(entityType);
                if (repo == null) {
                    QSchema schema = super.loadSchema(entityType);
                    repo = (JpaTable)createRepository(schema);
                }
            }
        }
        return repo;
    }

    public static <T,ID> JpaTable<T,ID> findRepository(Class<T> entityType) {
        JpaTable repo = jpaRepositories.get(entityType);
        return repo;
    }


    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }


    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return super.createSqlGenerator(isNativeQuery);
    }
}
