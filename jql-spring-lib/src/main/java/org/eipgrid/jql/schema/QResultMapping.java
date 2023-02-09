package org.eipgrid.jql.schema;

import java.util.List;

public interface QResultMapping {
    QResultMapping getParentNode();

    QSchema getSchema();

    String getMappingAlias();

    QJoin getEntityJoin();

    List<QColumn> getSelectedColumns();

    String[] getEntityMappingPath();

    boolean isArrayNode();

    boolean hasArrayDescendantNode();

    boolean isEmpty();

    QResultMapping getChildMapping(String name);
}
