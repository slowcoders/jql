package org.eipgrid.jql.jdbc.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.jdbc.output.BatchPreparedStatementSetterWithKeyHolder;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BatchUpsert<ID> implements BatchPreparedStatementSetterWithKeyHolder {
    private final Map<String, Object>[] entities;
    private final List<QColumn> columns;
    private final String sql;
    private final QSchema schema;
    private List<Map<String, Object>> generatedKeys;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public BatchUpsert(JdbcSchema schema, Collection<Map<String, Object>> entities, JqlEntitySet.InsertPolicy insertPolicy) {
        QueryGenerator gen = schema.getStorage().createQueryGenerator();
        this.sql = gen.prepareBatchInsertStatement(schema, insertPolicy);
        this.schema = schema;
        this.columns = schema.getWritableColumns();
        this.entities = entities.toArray(new Map[entities.size()]);
    }

    public String getSql() {
        return sql;
    }

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Map<String, Object> entity = entities[i];
        int idx = 0;
        for (QColumn col : columns) {
            Object json_v = entity.get(col.getJsonKey());
            Object value = convertJsonValueToColumnValue(col, json_v);
            ps.setObject(++idx, value);
        }
        ps.getGeneratedKeys();
    }

    static Object convertJsonValueToColumnValue(QColumn col, Object v) {
        if (v == null) return null;

        if (col.isJsonNode()) {
            if ((v instanceof Map) || (v instanceof Collection)) {
                try {
                    v = objectMapper.writeValueAsString(v);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return v.toString();
        }

        if (v.getClass().isEnum()) {
            if (Number.class.isAssignableFrom(col.getValueType())) {
                return ((Enum) v).ordinal();
            } else {
                return v.toString();
            }
        }
        return v;
    }


    @Override
    public int getBatchSize() {
        return entities.length;
    }

    @Override
    public void setGeneratedKeys(List<Map<String, Object>> keys) {
        this.generatedKeys = keys;
    }

    public List<ID> getEntityIDs() {
        int cntKeys = generatedKeys == null ? 0 : generatedKeys.size();
        List<QColumn> pkColumns = schema.getPKColumns();
        ArrayList<ID> ids = new ArrayList<>();
        for (int i = 0; i < entities.length; i++) {
            ID id = (ID)extractEntityId(pkColumns, entities[i], i < cntKeys ? this.generatedKeys.get(i) : null);
            ids.add(id);
        }
        return ids;
    }

    private static Object extractEntityId(List<QColumn> pkColumns, Map<String, Object> entity, Map<String, Object> generatedKeys) {
        if (pkColumns.size() > 1) {
            Object[] id = new Object[pkColumns.size()];
            int i = 0;
            for (QColumn pk : pkColumns) {
                Object v = getValue(pk.getJsonKey(), entity, generatedKeys);
                id[i++] = v;
            }
            return id;
        }
        else {
            Object id = getValue(pkColumns.get(0).getJsonKey(), entity, generatedKeys);
            return id;
        }
    }

    private static Object getValue(String key, Map<String, Object> entity, Map<String, Object> generatedKeys) {
        if (generatedKeys != null && generatedKeys.containsKey(key)) {
            return generatedKeys.get(key);
        }
        return entity.get(key);
    }


}
