package org.eipgrid.jql.jdbc.output;

import org.eipgrid.jql.schema.QResultMapping;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class JQRowMapper2 implements ResultSetExtractor<List<JqlEntity>> {
    private final List<QResultMapping> resultMappings;
    private MappedColumn[] mappedColumns;

    public JQRowMapper2(List<QResultMapping> rowMappings) {
        this.resultMappings = rowMappings;
    }

    private static class CacheNode extends HashMap<CacheNode.Key, CacheNode> {
        JqlEntity cachedEntity;

        public CacheNode(JqlEntity cachedEntity) {
            this.cachedEntity = cachedEntity;
        }

        static class Key {
            Object[] pks;

            Key(Object[] pks)  {
                this.pks = pks;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Key other = (Key) o;
                return Arrays.equals(pks, other.pks);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(pks);
            }
        }
    }

    @Override
    public List<JqlEntity> extractData(ResultSet rs) throws SQLException, DataAccessException {

        initMappedColumns(rs);

        ArrayList<JqlEntity> results = new ArrayList<>();
        HashMap<CacheNode.Key, CacheNode> rootEntityCache = new HashMap<>();
        CacheNode.Key searchKey = new CacheNode.Key(null);

        while (rs.next()) {
            int idxColumn = 0;
            int columnCount = mappedColumns.length;
            JqlEntity baseEntity = null;
            int lastMappingIndex = resultMappings.size() - 1;

            HashMap<CacheNode.Key, CacheNode> cacheNodes = rootEntityCache;
            for (int i = 0; i < lastMappingIndex; i++) {
                QResultMapping mapping = resultMappings.get(i);
                if (!mapping.hasArrayDescendantNode()) break;

                Object[] pk = readPrimaryKeys(mapping, rs, idxColumn);
                if (pk == null) {
                    columnCount = idxColumn;
                    break;
                }
                searchKey.pks = pk;
                CacheNode cacheNode = cacheNodes.get(searchKey);
                boolean cacheFound = cacheNode != null;
                if (cacheNode == null) {
                    JqlEntity newEntity = readEntity(mapping, baseEntity, rs, idxColumn);
                    cacheNode = new CacheNode(newEntity);
                    cacheNodes.put(new CacheNode.Key(pk), cacheNode);
                    if (i == 0) {
                        results.add(newEntity);
                        baseEntity = cacheNode.cachedEntity;
                    };
                }
                if (i == 0) {
                    baseEntity = cacheNode.cachedEntity;
                }
                cacheNodes = cacheNode;
                idxColumn += mapping.getSelectedColumns().size();
                if (!cacheFound) break;
            }

            if (idxColumn == 0) {
                baseEntity = new JqlEntity();
                results.add(baseEntity);
            }
            JqlEntity entity = baseEntity;
            QResultMapping mapping = mappedColumns[0].mapping;
            for (; idxColumn < columnCount; ) {
                MappedColumn mappedColumn = mappedColumns[idxColumn];
                if (mapping != mappedColumn.mapping) {
                    mapping = mappedColumn.mapping;
                    entity = makeSubEntity(baseEntity, mapping);
                }
                mappedColumn.value = getColumnValue(rs, ++idxColumn);
                entity.putIfAbsent(mappedColumn.fieldName, mappedColumn.value);
            }
        }
        return results;
    }

    private JqlEntity makeSubEntity(JqlEntity entity, QResultMapping mapping) {
        if (entity == null) {
            return new JqlEntity();
        }
        String[] entityPath = mapping.getEntityMappingPath();
        int idxLastPath = entityPath.length - 1;
        for (int i = 0; i < idxLastPath; i++) {
            entity = makeSubEntity(entity, entityPath[i], false);
        }
        entity = makeSubEntity(entity, entityPath[idxLastPath], mapping.isArrayNode());
        return entity;
    }

    private JqlEntity readEntity(QResultMapping mapping, JqlEntity baseEntity, ResultSet rs, int idxColumn) throws SQLException {
        JqlEntity entity = makeSubEntity(baseEntity, mapping);
        for (int i = mapping.getSelectedColumns().size(); --i >= 0; ) {
            MappedColumn mc = mappedColumns[idxColumn];
            Object v = getColumnValue(rs, ++idxColumn);
            entity.put(mc.fieldName, v);
        }
        return entity;
    }

    private Object[] readPrimaryKeys(QResultMapping mapping, ResultSet rs, int pkIndex) throws SQLException {
        List<QColumn> pkColumns = mapping.getSchema().getPKColumns();
        Object[] pks = new Object[pkColumns.size()];
        for (int idxPk = 0; idxPk < pks.length; idxPk++) {
            Object value = getColumnValue(rs, ++pkIndex);
            if (value == null) return null;
            pks[idxPk] = value;
        }
        return pks;
    }

    private JqlEntity makeSubEntity(JqlEntity entity, String key, boolean isArray) {
        Object subEntity = entity.get(key);
        if (subEntity == null) {
            subEntity = new JqlEntity();
            if (isArray) {
                ArrayList<Object> array = new ArrayList<>();
                array.add(subEntity);
                entity.put(key, array);
            } else {
                entity.put(key, subEntity);
            }
        } else if (isArray) {
            ArrayList<Object> array = (ArrayList<Object>) subEntity;
            subEntity = new JqlEntity();
            array.add(subEntity);
        } else if (subEntity instanceof ArrayList) {
            ArrayList<JqlEntity> list = (ArrayList<JqlEntity>) subEntity;
            subEntity = list.get(list.size()-1);
        }
        return (JqlEntity)subEntity;
    }

    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }


    private void initMappedColumns(ResultSet rs) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        MappedColumn[] mappedColumns = new MappedColumn[columnCount];
        ColumnMappingHelper helper = new ColumnMappingHelper();

        int idxColumn = 0;
        for (QResultMapping mapping : resultMappings) {
            helper.reset(mapping.getEntityMappingPath());
            for (QColumn column : mapping.getSelectedColumns()) {
                String[] path = helper.getEntityMappingPath(column);
                mappedColumns[idxColumn++] = new MappedColumn(mapping, column, path);
            }
        }
        if (idxColumn != columnCount) {
            throw new RuntimeException("Something wrong!");
        }
        this.mappedColumns = mappedColumns;
    }


    private static class ColumnMappingHelper extends HashMap<String, ColumnMappingHelper> {
        String[] entityPath;

        void reset(String[] jsonPath) {
            this.entityPath = jsonPath;
            this.clear();
        }

        public String[] getEntityMappingPath(QColumn column) {
            String jsonKey = column.getJsonKey();
            ColumnMappingHelper cache = this;
            for (int p; (p = jsonKey.indexOf('.')) > 0; ) {
                cache = cache.register(entityPath, jsonKey.substring(0, p));
                jsonKey = jsonKey.substring(p + 1);
            }
            return cache.entityPath;

        }

        public ColumnMappingHelper register(String[] basePath, String key) {
            ColumnMappingHelper cache = this.get(key);
            if (cache == null) {
                cache = new ColumnMappingHelper();
                cache.entityPath = toJsonPath(basePath, key);
            }
            return cache;
        }

        private String[] toJsonPath(String[] basePath, String key) {
            String[] jsonPath = new String[basePath.length + 1];
            System.arraycopy(basePath, 0, jsonPath, 0, basePath.length);
            jsonPath[basePath.length] = key;
            return jsonPath;
        }
    }

    private static class MappedColumn {
        final QColumn column;
        final QResultMapping mapping;
        final String[] mappingPath;
        final String fieldName;
        Object value;

        public MappedColumn(QResultMapping mapping, QColumn column, String[] path) {
            this.mapping = mapping;
            this.column = column;
            this.mappingPath = path;
            this.fieldName = QSchema.getJavaFieldName(column);
        }
    }
}
