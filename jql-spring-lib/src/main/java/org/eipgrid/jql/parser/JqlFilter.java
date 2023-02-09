package org.eipgrid.jql.parser;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QResultMapping;

import java.util.ArrayList;
import java.util.List;

public class JqlFilter extends TableFilter {

    private final ArrayList<QResultMapping> columnGroupMappings = new ArrayList<>();
    private int cntMappingAlias;

    private boolean selectAuto;
    private boolean enableJPQL;

    public JqlFilter(QSchema schema) {
        super(schema, "t_0");
        enableJPQL = schema.isJPARequired();
    }

    public static <ID> JqlFilter of(QSchema schema, ID id) {
        JqlFilter filter = new JqlFilter(schema);
        QColumn first_pk = schema.getPKColumns().get(0);
        filter.getPredicateSet().add(PredicateFactory.IS.createPredicate(first_pk, id));
        return filter;
    }

    public static <ID> JqlFilter of(QSchema schema, List<ID> idList) {
        JqlFilter filter = new JqlFilter(schema);
        QColumn first_pk = schema.getPKColumns().get(0);
        filter.getPredicateSet().add(PredicateFactory.IS.createPredicate(first_pk, idList));
        return filter;
    }


    public void setSelectedProperties(List<String> keys) {
        selectAuto = (keys == null || keys.size() == 0);
        if (selectAuto) {
            this.addSelection("*");
            return;
        }

        for (String k : keys) {
            this.addSelection(k.trim());
        }
    }

    private void addSelection(String key) {
        EntityFilter scope = this;
        for (int p; (p = key.indexOf('.')) > 0; ) {
            QSchema schema = scope.getSchema();
            if (schema != null && schema.hasColumn(key)) {
                break;
            }
            String token = key.substring(0, p);
            scope = scope.makeSubNode(token, JqlParser.NodeType.Entity);
            key = key.substring(p + 1);
        }

        TableFilter table = scope.asTableFilter();
        if (table != null) {
            if (table.getSchema().getEntityJoinBy(key) != null) {
                scope = table.makeSubNode(key, JqlParser.NodeType.Leaf).asTableFilter();
                key = "*";
            }
        }
        scope.addSelectedColumn(key);
    }

    public JqlFilter getRootNode() {
        return this;
    }

    @Override
    public boolean isArrayNode() {
        return true;
    }

    public List<QResultMapping> getResultMappings() {
        if (columnGroupMappings.size() == 0) {
            gatherColumnMappings(columnGroupMappings);
        }
        return columnGroupMappings;
    }

    String createUniqueMappingAlias() {
        cntMappingAlias ++;
        if (cntMappingAlias < 10) {
            return "t_" + cntMappingAlias;
        } else {
            return "t" + cntMappingAlias;
        }
    }

    public boolean isJPQLEnabled() {
        return this.enableJPQL;
    }

    boolean isSelectAuto() {
        return selectAuto;
    }

    public void disableJPQL() {
        this.enableJPQL = false;
    }
//    public List<JQColumn> resolveSelectedColumns(TableFilter tableFilter) {
//        if (!selectAuto) return Collections.EMPTY_LIST;
//
//        JQSchema schema = tableFilter.getSchema();
//        List<JQColumn> columns = tableFilter.getSchema().getReadableColumns();
//
//        Set<JQColumn> hiddenKeys = tableFilter.getHiddenForeignKeys();
//        if (!hiddenKeys.isEmpty()) {
//            ArrayList<JQColumn> columns2 = new ArrayList<>();
//            for (JQColumn column : tableFilter.getSelectedColumns()) {
//                if (hiddenKeys.contains(column)) continue;
//                columns2.add(column);
//            }
//            columns = columns2;
//        }
//
//        return columns;
//    }
}
