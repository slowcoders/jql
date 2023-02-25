package org.eipgrid.jql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.parser.JqlParser;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.*;

public abstract class JqlRepository<ENTITY, ID> implements JqlEntitySet<ENTITY, ID> {

    protected final QSchema schema;
    protected JqlParser jqlParser;

    public static final Class<Map> RawEntityType = Map.class;

    protected JqlRepository(QSchema schema, ObjectMapper objectMapper) {
        this.schema = schema;
        this.jqlParser = new JqlParser(objectMapper);
    }


    public abstract JqlStorage getStorage();

    public final String getTableName() {
        return schema.getTableName();
    }

    public final QSchema getSchema() { return schema; }

    public final Class<ENTITY> getEntityType() { return (Class<ENTITY>)schema.getEntityType(); }

    public abstract ID convertId(Object id);

    public final boolean hasGeneratedId() {
        return schema.hasGeneratedId();
    }

    public abstract  <T> List<T> find(Iterable<ID> idList, JqlSelect select, Class<T> entityType);


    public abstract <T> T find(ID id, JqlSelect select, Class<T> entityType);

    public List<ENTITY> findAll(JqlSelect select, Sort sort) {
        JqlQuery query = createQuery(null);
        query.select(select);
        query.sort(sort);
        return find(query, getEntityType());
    }


    public List<ENTITY> find(JqlQuery query, OutputFormat outputType) {
        return find(query);
    }


    public abstract <T> List<T> find(JqlQuery query, Class<T> entityType);
    public List<ENTITY> find(JqlQuery query) { return find(query, getEntityType()); }

    public List<Map<String, Object>> find_raw(JqlQuery query) { return (List)find(query, RawEntityType); }


    public Map<String, Object> find_raw(ID id, JqlSelect select) { return find(id, select, RawEntityType); }

    public ENTITY find(ID id, JqlSelect select) { return find(id, select, getEntityType()); }

    public ENTITY find(ID id) { return find(id, null); }

    public Map<String, Object> find_raw(ID id) { return find_raw(id, null); }


    public <T> T get(ID id, JqlSelect select, Class<T> entityType) {
        T entity = find(id, select, entityType);
        if (entity == null) throw new IllegalArgumentException(getEntityType().getSimpleName() +
                " not found: " + id);
        return entity;
    }

    public ENTITY get(ID id, JqlSelect select) { return get(id, select, getEntityType()); }

    public ENTITY get(ID id) { return get(id, null); }

    public Map<String, Object> get_raw(ID id, JqlSelect select) { return get(id, select, RawEntityType); }

    public Map<String, Object> get_raw(ID id) { return get_raw(id, null); }



    public List<ENTITY> find(Iterable<ID> idList, JqlSelect select) { return find(idList, select, getEntityType()); }

    public List<ENTITY> find(Iterable<ID> idList) { return find(idList, null); }

    public List<Map<String, Object>> find_raw(Collection<ID> idList, JqlSelect select) { return (List)find(idList, select, RawEntityType); }

    public List<Map<String, Object>> find_raw(Collection<ID> idList) { return (List)find(idList, null); }




    public abstract List<ID> insert(Collection<? extends Map<String, Object>> entities);

    public abstract void update(Iterable<ID> idList, Map<String, Object> updateSet) throws IOException;

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
