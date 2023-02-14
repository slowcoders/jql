package org.eipgrid.jql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.jdbc.QueryGenerator;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.jpa.JPARepositoryBase;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.eipgrid.jql.util.CaseConverter;
import org.eipgrid.jql.util.ClassUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.HashMap;

@Service
public abstract class JqlStorage implements CaseConverter {
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final PhysicalNamingStrategy namingStrategy;
    private final ConversionService conversionService;

    private HashMap<String, JqlRepository> repositories = new HashMap<>();
    private HashMap<String, JPARepositoryBase> jpaRepositories = new HashMap<>();

    public JqlStorage(DataSource dataSource,
                      TransactionTemplate transactionTemplate,
                      ConversionService conversionService,
                      EntityManager entityManager) throws Exception {
        this.jdbc = new JdbcTemplate(dataSource);
        this.objectMapper = new ObjectMapper();
        // objectMapper.registerModule(jqlModule);
        this.transactionTemplate = transactionTemplate;
        this.conversionService = conversionService;
        this.entityManager = entityManager;
        String cname = (String) entityManager.getEntityManagerFactory().getProperties().get("hibernate.physical_naming_strategy");
        this.namingStrategy = ClassUtils.newInstanceOrNull(cname);
        System.out.println(cname);
    }

    public abstract QueryGenerator createQueryGenerator(boolean isNativeQuery);

    public final QueryGenerator createQueryGenerator() { return createQueryGenerator(true); }

    public JdbcTemplate getJdbcTemplate() {
        return jdbc;
    }

    public EntityManager getEntityManager() { return entityManager; }

    public ConversionService getConversionService() {
        return this.conversionService;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public abstract JqlRepository getRepository(String tableName);

    public abstract <T, ID> JPARepositoryBase<T, ID> getRepository(Class<T> entityType);

    public abstract QSchema loadSchema(String tableName);

    public abstract QSchema loadSchema(Class entityType);

    public String makeTablePath(String schema, String name) {
        name = schema + "." + name;
        return name;
    }

    public String toPhysicalColumnName(String fieldName) {
        Identifier physicalName = this.namingStrategy.toPhysicalColumnName(Identifier.toIdentifier(fieldName, false), null);
        return physicalName.getCanonicalName();
    }

    @Override
    public String toLogicalAttributeName(String columnName) {
        throw new RuntimeException("not implemented");
    }

    public DataSource getDataSource() {
        return this.jdbc.getDataSource();
    }

    public TransactionTemplate getTransactionTemplate() {
        return this.transactionTemplate;
    }

}
