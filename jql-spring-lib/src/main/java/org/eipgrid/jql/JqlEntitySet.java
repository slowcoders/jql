package org.eipgrid.jql;

import org.eipgrid.jql.parser.JqlFilter;

import java.io.IOException;
import java.util.*;

public interface JqlEntitySet<ENTITY, ID> {

    JqlFilter createFilter(Map<String, Object> filter);

    ID getEntityId(ENTITY entity);

    List<ENTITY> find(JqlQuery query, OutputFormat outputType);

    List<ENTITY> find(Collection<ID> idList);
    default ENTITY find(ID id, JqlSelect select) {
        List<ENTITY> res = find(Collections.singletonList(id));
        return res.size() > 0 ? res.get(0) : null;
    }
    default ENTITY find(ID id) {
        return find(id, null);
    }

    long count(JqlFilter filter);


    List<ID> insert(Collection<Map<String, Object>> entities) throws IOException;
    ENTITY insert(Map<String, Object> properties) throws IOException;

    void update(Collection<ID> idList, Map<String, Object> properties) throws IOException;
    default void update(ID id, Map<String, Object> updateSet) throws IOException {
        update(Collections.singletonList(id), updateSet);
    }

    void delete(Collection<ID> idList);
    default void delete(ID id) {
        delete(Collections.singletonList(id));
    }

}