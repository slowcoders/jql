package org.eipgrid.jql.parser;

import org.eipgrid.jql.JqlSelect;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QResultMapping;
import org.eipgrid.jql.schema.QSchema;

import java.util.*;

class TableFilter extends EntityFilter implements QResultMapping {
    private final QSchema schema;
    private final QJoin join;
    private final String mappingAlias;

    private String[] entityMappingPath;
    private List<QColumn> selectedColumns = null;
    private boolean hasArrayDescendant;

    private static final String[] emptyPath = new String[0];

    TableFilter(QSchema schema, String mappingAlias) {
        super(null);
        this.schema = schema;
        this.join = null;
        this.entityMappingPath = emptyPath;
        this.mappingAlias = mappingAlias;
    }

    TableFilter(TableFilter baseFilter, QJoin join) {
        super(baseFilter);
        this.schema = join.getTargetSchema();
        this.join = join;
        this.mappingAlias = baseFilter.getRootNode().createUniqueMappingAlias();
    }

    public QSchema getSchema() {
        return schema;
    }

    TableFilter asTableFilter() {
        return this;
    }

    public TableFilter getParentNode() {
        EntityFilter parent = super.getParentNode();
        return parent == null ? null : parent.asTableFilter();
    }

    public boolean hasChildMappings() { return !subFilters.isEmpty(); }

    @Override
    public String getMappingAlias() {
        return mappingAlias;
    }

    public String[] getEntityMappingPath() {
        String[] jsonPath = this.entityMappingPath;
        if (jsonPath == null) {
            TableFilter parent = getParentNode();
            String[] basePath = parent.getEntityMappingPath();
            jsonPath = new String[basePath.length + 1];
            System.arraycopy(basePath, 0, jsonPath, 0, basePath.length);
            String lastPath = join.getJsonKey();
            jsonPath[jsonPath.length - 1] = lastPath;
            this.entityMappingPath = jsonPath;
        }
        return jsonPath;
    }


    public String getTableName() {
        return schema.getTableName();
    }

    @Override
    public List<QColumn> getSelectedColumns() {
        if (selectedColumns == null) {
            if (!getRootNode().isSelectAuto()) {
                this.selectedColumns = Collections.EMPTY_LIST;
            }
            else {
                this.selectedColumns = schema.getLeafColumns();
                if (this.schema.getObjectColumns().size() > 0) {
                    for (Map.Entry<String, EntityFilter> entry : this.subFilters.entrySet()) {
                        EntityFilter filter = entry.getValue();
                        if (filter.isJsonNode()) {
                            addSelectedColumn(entry.getKey());
                        }
                    }
                }
            }
        }
        return selectedColumns;
    }

    @Override
    protected EntityFilter makeSubNode(String key, JqlParser.NodeType nodeType) {
        QJoin join = schema.getEntityJoinBy(key);
        QColumn jsonColumn = null;
        if (join == null) {
            jsonColumn = schema.getColumn(key);
            if (!jsonColumn.isJsonNode()) return this;
        }

        EntityFilter subQuery = subFilters.get(key);
        if (subQuery == null) {
            if (join != null) {
                subQuery = new TableFilter(this, join);
            } else {
                subQuery = new JsonFilter(this, jsonColumn.getPhysicalName());
                this.addSelectedColumn(jsonColumn);
            }
            subFilters.put(key, subQuery);
        }
        return subQuery;
    }

    private void addSelectedColumn(QColumn column) {
        if (this.selectedColumns == null) {
            this.selectedColumns = isArrayNode() ? new ArrayList<>(schema.getPKColumns()) : new ArrayList<>();
        }

        if (this.selectedColumns.contains(column)) return;

        if (!(this.selectedColumns instanceof ArrayList)) {
            // makes mutable!!
            this.selectedColumns = new ArrayList<>(this.selectedColumns);
        }
        this.selectedColumns.add(column);
    }

    @Override
    protected String getColumnName(String key) {
        while (!this.schema.hasColumn(key)) {
            int p = key.indexOf('.');
            if (p < 0) {
                throw new IllegalArgumentException("invalid key: " + key);
            }
            key = key.substring(p + 1);
        }
        return this.schema.getColumn(key).getPhysicalName();
    }

    public QJoin getEntityJoin() {
        return this.join;
    }

    @Override
    public boolean isArrayNode() {
        return !this.join.hasUniqueTarget();
    }

    @Override
    public boolean hasArrayDescendantNode() {
        return this.hasArrayDescendant;
    }

    protected void gatherColumnMappings(List<QResultMapping> columnGroupMappings) {
        columnGroupMappings.add(this);
        this.hasArrayDescendant = false;
        for (EntityFilter q : subFilters.values()) {
            TableFilter table = q.asTableFilter();
            if (table != null) {
                table.gatherColumnMappings(columnGroupMappings);
                this.hasArrayDescendant |= table.isArrayNode() || table.hasArrayDescendant;
            } else if (!q.isEmpty()) {
                getRootNode().disableJPQL();
            }
        }
    }

    protected void addSelection(JqlSelect.ResultMap resultMap) {
        QSchema schema = this.getSchema();
        boolean allLeaf = resultMap.isAllLeafSelected();
        if (allLeaf) {
            this.selectedColumns = schema.getLeafColumns();
        } else if (this.isArrayNode() || resultMap.isIdSelected()) {
            this.selectedColumns = schema.getPKColumns();
        }

        for (Map.Entry<String, JqlSelect.ResultMap> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            QColumn column = schema.findColumn(key);
            if (column != null) {
                if (!allLeaf) this.addSelectedColumn(column);
            }
            else {
                EntityFilter scope = this.makeSubNode(key, JqlParser.NodeType.Entity);
                scope.addSelection(entry.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return join != null ? join.getJsonKey() : schema.getTableName();
    }

}
