package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.JqlSelect;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.jdbc.JdbcRepositoryBase;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JqlAdapter<ENTITY, ID> implements JqlEntitySet<ENTITY, ID> {
    protected final JdbcRepositoryBase<ID> repository;
    protected final Class<ENTITY> entityType;
    protected final ObjectMapper objectMapper;

    public JqlAdapter(JdbcRepositoryBase<ID> repository, Class<ENTITY> entityType, ObjectMapper objectMapper) {
        this.repository = repository;
        this.entityType = entityType;
        this.objectMapper = objectMapper;
    }
    public JqlAdapter(JdbcRepositoryBase<ID> repository, Class<ENTITY> entityType) {
        this(repository, entityType, repository.getStorage().getObjectMapper());
    }

    public JdbcRepositoryBase<ID> getRepository() {
        return this.repository;
    }
    protected JqlAdapter(JdbcStorage storage, Class<ENTITY> entityType) {
        this.entityType = entityType;
        this.repository = storage.registerTable(this, entityType);
        this.objectMapper = storage.getObjectMapper();
    }

    public final Class<ENTITY> getEntityType() {
        return this.entityType;
    }

    @Override
    public JqlQuery<ENTITY> createQuery(Map<String, Object> jqlFilter) {
        return (JqlQuery<ENTITY>)repository.createQuery(jqlFilter);
    }

    @Override
    public ENTITY find(ID id, JqlSelect select) {
        Map raw_entity = repository.find(id, select);
        ENTITY entity = convertToEntity(raw_entity);
        return entity;
    }

    public ENTITY convertToEntity(Map rawEntity) {
        if (rawEntity == null) return null;
        ENTITY entity = objectMapper.convertValue(rawEntity, entityType);
        return entity;
    }

    private List<ENTITY> replaceContentToEntities(List res) {
        if (res.size() == 0 || res.get(0).getClass().isAssignableFrom(entityType)) {
            return res;
        }
        for (int i = res.size(); --i >= 0; ) {
            ENTITY v = convertToEntity((Map)res.get(i));
            res.set(i, v);
        }
        return res;
    }

    @Override
    public List<ENTITY> find(Iterable<ID> idList, JqlSelect select) {
        List res = repository.find(idList, select);
        return replaceContentToEntities(res);
    }

    @Override
    public List<ENTITY> findAll(JqlSelect select, Sort sort) {
        List res = repository.findAll(select, sort);
        return replaceContentToEntities(res);
    }


    @Override
    public List<ID> insert(Collection<? extends Map<String, Object>> entities) {
        return repository.insert(entities);
    }

    @Override
    public ENTITY insert(Map<String, Object> properties) {
        Map res =  repository.insert(properties);
        return convertToEntity(res);
    }

    @Override
    public void update(Iterable<ID> idList, Map<String, Object> properties) {
        repository.update(idList, properties);
    }

    @Override
    public void update(ID id, Map<String, Object> updateSet) {
        JqlEntitySet.super.update(id, updateSet);
    }

    @Override
    public void delete(Iterable<ID> idList) {
        repository.delete(idList);
    }

    @Override
    public void delete(ID id) {
        JqlEntitySet.super.delete(id);
    }
}