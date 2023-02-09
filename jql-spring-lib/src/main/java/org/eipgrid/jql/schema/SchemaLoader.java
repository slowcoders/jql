package org.eipgrid.jql.schema;

import org.eipgrid.jql.util.CaseConverter;

import java.util.HashMap;

public abstract class SchemaLoader {

    private final CaseConverter nameConverter;

    protected SchemaLoader(CaseConverter nameConverter) {
        this.nameConverter = nameConverter;
    }

    public final CaseConverter getNameConverter() {
        return this.nameConverter;
    }

    public abstract QSchema loadSchema(String tablePath);

    public abstract QSchema loadSchema(Class<?> entityType);

    protected abstract HashMap<String, QJoin> loadJoinMap(QSchema schema);
}
