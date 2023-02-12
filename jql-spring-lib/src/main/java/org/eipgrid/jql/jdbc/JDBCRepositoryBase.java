package org.eipgrid.jql.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.*;
import org.eipgrid.jql.jdbc.output.ArrayRowMapper;
import org.eipgrid.jql.jdbc.output.IdListMapper;
import org.eipgrid.jql.jdbc.output.JsonRowMapper;
import org.eipgrid.jql.parser.JqlFilter;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JDBCRepositoryBase<ENTITY, ID> extends JqlRepository<ENTITY, ID> {
    private final JdbcTemplate jdbc;
    private final IdListMapper<ID> idListMapper = new IdListMapper<>(this);

    private static final ArrayRowMapper arrayMapper = new ArrayRowMapper();

    protected JDBCRepositoryBase(JqlStorage storage, QSchema schema) {
        super(storage, schema);
        this.jdbc = storage.getJdbcTemplate();
    }

    //    @Override
    protected ResultSetExtractor<List<Map>> getColumnMapRowMapper(JqlFilter filter) {
        return new JsonRowMapper(filter.getResultMappings(), storage.getObjectMapper());
    }

    public <T> List<T> find(JqlQuery query, Class<T> entityType) {
        boolean enableJPA = query.getFilter().isJPQLEnabled() && entityType == this.getEntityType();
        boolean isRepeat = (query.getExecutedQuery() != null && (Boolean)enableJPA == query.getExtraInfo());

        String sql = isRepeat ? query.getExecutedQuery() : storage.createQueryGenerator(!enableJPA).createSelectQuery(query);
        List res;
        if (enableJPA) {
            res = storage.getEntityManager().createQuery(sql).getResultList();
        }
        else {
            res = jdbc.query(sql, getColumnMapRowMapper(query.getFilter()));
            if (!RawEntityType.isAssignableFrom(entityType)) {
                ObjectMapper converter = storage.getObjectMapper();
                for (int i = res.size(); --i >= 0; ) {
                    T v = (T)converter.convertValue(res.get(i), entityType);
                    res.set(i, v);
                }
            }
        }
        super.setGenerateQuery(query, sql, enableJPA);
        return res;
    }

    @Override
    public List<?> find(JqlQuery query, OutputFormat outputType) {
        return find(query);
    }


    @Override
    public long count(JqlFilter filter) {
        String sqlCount = storage.createQueryGenerator().createCountQuery(filter);
        long count = jdbc.queryForObject(sqlCount, Long.class);
        return count;
    }

    // Insert Or Update Entity
    @Override
    public List<ID> insert(Collection<Map<String, Object>> entities) {
        if (entities.isEmpty()) return null;

        BatchUpsert batch = new BatchUpsert(this.getSchema(), entities, true);
        jdbc.batchUpdate(batch.getSql(), batch);
        return batch.getEntityIDs();
    }


    @Override
    public void update(Collection<ID> idList, Map<String, Object> updateSet) throws IOException {
        JqlFilter filter = JqlFilter.of(schema, idList);
        String sql = storage.createQueryGenerator().createUpdateQuery(filter, updateSet);
        jdbc.update(sql);
    }

    @Override
    public void delete(ID id) {
        JqlFilter filter = JqlFilter.of(schema, id);
        String sql = storage.createQueryGenerator().createDeleteQuery(filter);
        jdbc.update(sql);
    }

    @Override
    public void delete(Collection<ID> idList) {
        JqlFilter filter = JqlFilter.of(schema, idList);
        String sql = storage.createQueryGenerator().createDeleteQuery(filter);
        jdbc.update(sql);
    }


    public void removeEntityCache(ID id) {
        // do nothing.
    }


    public ID convertId(Object v) {
        /** 참고. 2023.01.31
         * PathVariable 또는 RequestParam 에 사용된 ID 는 ConversionService 를 통해서 parsing 된다.
         * 해당 ID 는 Jql 검색을 통해 얻은 Json Value 값이므로, ObjectMapper 를 통한 Parsing 또한 가능하나,
         * StorageController 와 TableController 동작 호환성을 위해 ConversionService 를 사용한다.
         */
        ConversionService cvtService = storage.getConversionService();
        List<QColumn> pkColumns = schema.getPKColumns();
        if (pkColumns.size() == 1) {
            return (ID)cvtService.convert(v, pkColumns.get(0).getValueType());
        }
        String pks[] = ((String)v).split("|");
        if (pks.length != pkColumns.size()) {
            throw new RuntimeException("invalid primary keys: " + v);
        }
        Object ids[] = new Object[pks.length];
        for (int i = 0; i < pks.length; i++) {
            ids[i] = cvtService.convert(pks[i], pkColumns.get(i).getValueType());
        }
        return (ID)ids;
    }
}