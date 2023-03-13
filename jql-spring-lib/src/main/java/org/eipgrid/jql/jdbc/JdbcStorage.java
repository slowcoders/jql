package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;
import org.eipgrid.jql.jdbc.storage.QueryGenerator;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.jpa.JqlAdapter;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.sql.DataSource;
import java.util.HashMap;

public class JdbcStorage extends JdbcSchemaLoader {
    private HashMap<String, JdbcRepositoryBase> repositories = new HashMap<>();
    private static HashMap<Class, JpaTable> jpaTables = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) {
        super(dataSource, transactionTemplate, objectMapper, entityManager);
    }

    public JdbcRepositoryBase registerTable(JqlAdapter table, Class entityType) {
        synchronized (jpaTables) {
            if (jpaTables.put(entityType, (JpaTable) table) != null) {
                throw new RuntimeException("Jpa repository already registered " + entityType.getName());
            }
            QSchema schema = super.loadSchema(entityType);
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
                    QSchema schema = super.loadSchema(tableName);
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


    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return super.createSqlGenerator(isNativeQuery);
    }

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
}
