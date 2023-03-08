package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.jdbc.storage.JdbcSchemaLoader;
import org.eipgrid.jql.jdbc.storage.QueryGenerator;
import org.eipgrid.jql.jpa.JpaAdapter;
import org.eipgrid.jql.jpa.JqlAdapter;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.sql.DataSource;
import java.util.HashMap;

public class JdbcStorage extends JdbcSchemaLoader {
    private HashMap<String, JdbcRepositoryBase> repositories = new HashMap<>();
    private static HashMap<Class, JpaAdapter> jpaTables = new HashMap<>();

    public JdbcStorage(DataSource dataSource,
                       TransactionTemplate transactionTemplate,
                       ObjectMapper objectMapper,
                       EntityManager entityManager) {
        super(dataSource, transactionTemplate, objectMapper, entityManager);
    }

    public JdbcRepositoryBase registerTable(JqlAdapter table, Class entityType) {
        synchronized (jpaTables) {
            if (jpaTables.put(entityType, (JpaAdapter) table) != null) {
                throw new RuntimeException("Jpa repository already registered " + entityType.getName());
            }
            QSchema schema = super.loadSchema(entityType);
            JdbcRepositoryBase repo = new JdbcRepositoryImpl(this, schema);
//            if (repositories.put(schema.getTableName(), repo) != null) {
//                throw new RuntimeException("Jpa repository already registered " + entityType.getName());
//            };
            return repo;
        }
    }

    protected void registerTable(JdbcRepositoryBase table) {

        synchronized (jpaTables) {
            QSchema schema = table.getSchema();
            JqlRepository old_table;

//            if (schema.isJPARequired()) {
//                old_table = jpaTables.put(schema.getEntityType(), (JpaAdapter) table);
//                if (old_table != null) {
//                    throw new Error("Duplicated JpaTable on this class " + schema.getEntityType().getName());
//                }
//            }

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
                repo = repositories.get(entityType);
            } else {
                repo = new JdbcRepositoryImpl(this, schema);
            }
        }
        return repo;
    }

    public <T, ID> JqlEntitySet<T, ID> getEntitySet(String tableName) {
        JdbcRepositoryBase repo = getRepository(tableName);
        JqlEntitySet table = jpaTables.get(repo.getSchema().getEntityType());
        if (table == null) table = repo;
        return table;
    }

    public JdbcRepositoryBase getRepository(String tableName) {
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

    public <T,ID> JpaAdapter<T,ID> getRepository(Class<T> entityType) {
        JpaAdapter table = jpaTables.get(entityType);
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

    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }


    public QueryGenerator createQueryGenerator(boolean isNativeQuery) {
        return super.createSqlGenerator(isNativeQuery);
    }

    private class JpaTableImpl<ENTITY, ID> extends JpaAdapter<ENTITY, ID> {
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
