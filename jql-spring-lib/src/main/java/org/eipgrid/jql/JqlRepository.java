package org.eipgrid.jql;

import org.eipgrid.jql.schema.QSchema;
import org.springframework.data.domain.Sort;

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

//    public abstract <T> List<T> findAll(JqlSelect select, Sort sort, Class<T> entityType);
//
//    public final List<Map> findAll(JqlSelect select, Sort sort) {
//        return findAll(select, sort, rawEntityType);
//    }
//    public abstract <T> List<T> find(Iterable<ID> idList, JqlSelect select, Class<T> entityType);
//
//    public final List<Map> find(Iterable<ID> idList, JqlSelect select) {
//        return find(idList, select, rawEntityType);
//    }
//
//    public abstract <T> T find(ID id, JqlSelect select, Class<T> entityType);
//
//    public final Map find(ID id, JqlSelect select) {
//        return find(id, select, rawEntityType);
//    }
//
//    public abstract <T> List<T> find(JqlQuery query0, Class<T> entityType);
//
//
//    public List<Map> find(JqlQuery query, OutputFormat outputType) {
//        return find(query, Map.class);
//    }



    public abstract List<ID> insert(Collection<? extends Map<String, Object>> entities);

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
