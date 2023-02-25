package org.eipgrid.jql;

import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.*;

public interface JqlEntitySet<ENTITY, ID> {

    JqlQuery<ENTITY> createQuery(Map<String, Object> filter);

    ENTITY find(ID id, JqlSelect select);

    List<ENTITY> find(Iterable<ID> idList, JqlSelect select);

    List<ENTITY> findAll(JqlSelect select, Sort sort);


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