package org.eipgrid.jql;

import org.eipgrid.jql.schema.QSchema;

import java.util.*;

public abstract class JqlRepository<ID> implements JqlEntitySet<Map, ID> {

    protected final QSchema schema;

    public static final Class<Map> rawEntityType = Map.class;

    protected JqlRepository(QSchema schema) {
        this.schema = schema;
    }


    public abstract JqlStorage getStorage();

    public final String getTableName() {
        return schema.getTableName();
    }

    public final QSchema getSchema() { return schema; }

    public abstract ID convertId(Object id);

    public final boolean hasGeneratedId() {
        return schema.hasGeneratedId();
    }

    public abstract List<ID> insert(Collection<? extends Map<String, Object>> entities, InsertPolicy insertPolicy);

    public abstract void update(Iterable<ID> idList, Map<String, Object> updateSet);

    public abstract void delete(Iterable<ID> idList);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JqlRepository that = (JqlRepository) o;
        return Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return schema.hashCode();
    }

}
