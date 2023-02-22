package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;
import org.eipgrid.jql.jdbc.storage.QueryGenerator;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.sql.DataSource;
import java.util.HashMap;

public class JdbcStorage extends JdbcSchemaLoader {
    private HashMap<String, JdbcTable> repositories = new HashMap<>();
    private static HashMap<Class, JpaTable> jpaTables = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) {
        super(dataSource, transactionTemplate, objectMapper, entityManager);
    }

    protected void registerTable(JdbcTable table) {

        synchronized (jpaTables) {
            QSchema schema = table.getSchema();
            JqlRepository old_table;

            if (schema.isJPARequired()) {
                old_table = jpaTables.put(schema.getEntityType(), (JpaTable) table);
                if (old_table != null) {
                    throw new Error("Duplicated JpaTable on this class " + schema.getEntityType().getName());
                }
            }

            String tableName = table.getTableName();
            old_table = repositories.put(tableName, table);
            if (old_table != null) {
                throw new Error("Duplicated JdbcTable on the table " + table.getTableName());
            }

        }
    }


    private JdbcTable createRepository(QSchema schema) {
        JdbcTable repo;
        synchronized (jpaTables) {
            if (schema.isJPARequired()) {
                Class<?> entityType = schema.getEntityType();
                repo = new JpaTableImpl(this, entityType);
            } else {
                repo = new JdbcTable(this, schema);
            }
        }
        return repo;
    }

    public JdbcTable getRepository(String tableName) {
        JdbcTable repo = repositories.get(tableName);
        if (repo == null) {
            synchronized (jpaTables) {
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
        JpaTable repo = jpaTables.get(entityType);
        if (repo == null) {
            QSchema schema = this.loadSchema(entityType);
            synchronized (jpaTables) {
                repo = jpaTables.get(entityType);
                if (repo == null) {
                    repo = (JpaTable)createRepository(schema);
                }
            }
        }
        return repo;
    }

    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }


    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return super.createSqlGenerator(isNativeQuery);
    }

    public static <T, ID> JpaTable<T, ID> findTable(Class<T> entityType) {
        return (JpaTable<T, ID>) jpaTables.get(entityType);
    }

    private class JpaTableImpl<ENTITY, ID> extends JpaTable<ENTITY, ID> {
        private final PersistenceUnitUtil persistenceUnitUtil;

        public JpaTableImpl(JdbcStorage jdbcStorage, Class<ENTITY> entityType) {
            super(jdbcStorage, entityType);
            persistenceUnitUtil = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        }
        public ID getEntityId(ENTITY entity) {
            ID id = (ID)persistenceUnitUtil.getIdentifier(entity);
            return id;
        }
    }
}
