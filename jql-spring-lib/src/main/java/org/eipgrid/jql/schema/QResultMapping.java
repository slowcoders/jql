package org.eipgrid.jql.schema;

import java.util.List;

public interface QResultMapping {
    QSchema getSchema();

    QResultMapping getParentNode();

    boolean hasChildMappings();

    String getMappingAlias();

    QJoin getEntityJoin();

    List<QColumn> getSelectedColumns();

    String[] getEntityMappingPath();

    boolean isArrayNode();

    boolean hasArrayDescendantNode();

    // 생략 가능.
    boolean isEmpty();

}
