package org.eipgrid.jql;

import org.eipgrid.jql.parser.JqlFilter;

import java.io.IOException;
import java.util.*;

public interface JqlTable<ID> {

    JqlFilter createFilter(Map<String, Object> filter);

    List<?> find(JqlQuery query, OutputFormat outputType);

    List<?> find(Collection<ID> idList);
    default Object find(ID id, JqlSelect select) {
        List<?> res = find(Collections.singletonList(id));
        return res.size() > 0 ? res.get(0) : null;
    }

    default Object find(ID id) {
        return find(id, null);
    }

    long count(JqlFilter filter);


    List<ID> insert(Collection<Map<String, Object>> entities) throws IOException;
    default ID insert(Map<String, Object> properties) throws IOException {
        return insert(Collections.singletonList(properties)).get(0);
    }

    void update(Collection<ID> idList, Map<String, Object> properties) throws IOException;
    default void update(ID id, Map<String, Object> updateSet) throws IOException {
        update(Collections.singletonList(id), updateSet);
    }

    void delete(Collection<ID> idList);
    default void delete(ID id) {
        delete(Collections.singletonList(id));
    }


    default void clearEntityCaches() {}
}