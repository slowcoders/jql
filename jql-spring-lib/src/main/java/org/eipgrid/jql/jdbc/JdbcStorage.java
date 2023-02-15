package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.jdbc.metadata.JdbcSchemaLoader;
import org.eipgrid.jql.jpa.JPARepositoryBase;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.HashMap;

public class JdbcStorage extends JdbcSchemaLoader {
    private HashMap<String, JDBCRepositoryBase> repositories = new HashMap<>();
    private HashMap<Class, JPARepositoryBase> jpaRepositories = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) throws Exception {
        super(dataSource, transactionTemplate, objectMapper, entityManager);
    }

    private JDBCRepositoryBase createRepository(QSchema schema) {
        JDBCRepositoryBase repo;
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

    public JDBCRepositoryBase getRepository(String tableName) {
        JDBCRepositoryBase repo = repositories.get(tableName);
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

    public <T,ID> JPARepositoryBase<T,ID> getRepository(Class<T> entityType) {
        JPARepositoryBase repo = jpaRepositories.get(entityType);
        if (repo == null) {
            synchronized (repositories) {
                repo = jpaRepositories.get(entityType);
                if (repo == null) {
                    QSchema schema = super.loadSchema(entityType);
                    repo = (JPARepositoryBase)createRepository(schema);
                }
            }
        }
        return repo;
    }


    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }


    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return super.createSqlGenerator(isNativeQuery);
    }
}
