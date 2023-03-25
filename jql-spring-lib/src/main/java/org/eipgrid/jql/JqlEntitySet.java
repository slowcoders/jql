package org.eipgrid.jql;

import org.springframework.data.domain.Sort;

import java.util.*;

public interface JqlEntitySet<ENTITY, ID> {

    enum InsertPolicy {
        ErrorOnConflict,
        IgnoreOnConflict,
        UpdateOnConflict,
    }

    JqlQuery<ENTITY> createQuery(Map<String, Object> jqlFilter);

    List<ENTITY> findAll(JqlSelect select, Sort sort);

    default long count(Map<String, Object> jqlFilter) {
        return createQuery(jqlFilter).count();
    }

    ENTITY find(ID id, JqlSelect select);

    default ENTITY find(ID id) { return find(id, null); }

    default ENTITY get(ID id) {
        ENTITY entity = find(id, null);
        if (entity == null) throw new IllegalArgumentException("Entity not found: id = " + id);
        return entity;
    }

    List<ENTITY> find(Iterable<ID> idList, JqlSelect select);

    List<ID> insert(Collection<? extends Map<String, Object>> entities, InsertPolicy insertPolicy);
    default List<ID> insert(Collection<? extends Map<String, Object>> entities) {
        return insert(entities, InsertPolicy.ErrorOnConflict);
    }
    default List<ID> insertOrUpdate(Collection<? extends Map<String, Object>> entities) {
        return insert(entities, InsertPolicy.UpdateOnConflict);
    }

    ENTITY insert(Map<String, Object> properties, InsertPolicy insertPolicy);
    default ENTITY insert(Map<String, Object> properties) {
        return insert(properties, InsertPolicy.ErrorOnConflict);
    }
    default ENTITY insertOrUpdate(Map<String, Object> properties) {
        return insert(properties, InsertPolicy.UpdateOnConflict);
    }

    void update(Iterable<ID> idList, Map<String, Object> properties);
    default void update(ID id, Map<String, Object> updateSet) {
        update(Collections.singletonList(id), updateSet);
    }

    void delete(Iterable<ID> idList);
    default void delete(ID id) {
        delete(Collections.singletonList(id));
    }

}