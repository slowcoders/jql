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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Service
public abstract class JqlStorage {
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    public JqlStorage(TransactionTemplate transactionTemplate,
                      ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }



    public TransactionTemplate getTransactionTemplate() {
        return this.transactionTemplate;
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
    }

}
