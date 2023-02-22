package org.eipgrid.jql;

import java.io.IOException;
import java.util.*;

public interface JqlEntitySet<ENTITY, ID> {

    JqlQuery createQuery(ID id, JqlSelect select);

    JqlQuery createQuery(Iterable<ID> idList, JqlSelect select);

    JqlQuery createQuery(Map<String, Object> filter, JqlSelect select);

    ID getEntityId(ENTITY entity);

    List<ID> insert(Collection<? extends Map<String, Object>> entities) throws IOException;
    
    ENTITY insert(Map<String, Object> properties) throws IOException;

    void update(Iterable<ID> idList, Map<String, Object> properties) throws IOException;
    default void update(ID id, Map<String, Object> updateSet) throws IOException {
        update(Collections.singletonList(id), updateSet);
    }

    void delete(Iterable<ID> idList);
    default void delete(ID id) {
        delete(Collections.singletonList(id));
    }

}