package org.eipgrid.jql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.jpa.JpaTable;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.CaseConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    public abstract List<String> getNamespaces();

    public abstract List<String> getTableNames(String namespace);

    public TransactionTemplate getTransactionTemplate() {
        return this.transactionTemplate;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public abstract <ID> JqlRepository<ID> loadRepository(String tableName);

    public abstract <T, ID> JqlEntitySet<T, ID> loadEntitySet(String tableName);

    public abstract <T, ID> JpaTable<T, ID> loadJpaTable(Class<T> entityType);

    public abstract QSchema loadSchema(String tableName);

    public abstract QSchema loadSchema(Class entityType);

    public String toPhysicalColumnName(String fieldName) {
        return CaseConverter.toSnakeCase(fieldName);
    }

    public String toLogicalAttributeName(String tableName, String columnName) {
        tableName = tableName.toLowerCase();
        columnName = columnName.toLowerCase();
        int name_start = 0;
        int p = tableName.length();
        if (columnName.length() > p + 1 && columnName.charAt(p) == '_' && columnName.startsWith(tableName)) {
            name_start = p + 1;
        }
        else if ((p = tableName.indexOf('_') + 1) > 1 && columnName.startsWith(tableName.substring(0, p))) {
            name_start = p;
        }
        if (name_start > 0) {
            if (name_start >= columnName.length()) {
                System.out.println("");
            }
            columnName = columnName.substring(name_start);
            if (!Character.isAlphabetic(columnName.charAt(0))) {
                columnName = '_' + columnName;
            }
        }
        return CaseConverter.toCamelCase(columnName, false);
    }

    public String toEntityClassName(String tableName, boolean capitalizeFirstLetter) {
        return CaseConverter.toCamelCase(tableName, capitalizeFirstLetter);
    }
}
