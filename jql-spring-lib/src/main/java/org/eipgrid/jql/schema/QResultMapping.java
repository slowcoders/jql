package org.eipgrid.jql.schema;

import java.util.List;

public interface QResultMapping {
    QSchema getSchema();

    QResultMapping getParentNode();

    QResultMapping getChildMapping(String name);

    boolean hasChildMappings();

    String getMappingAlias();

    QJoin getEntityJoin();

    List<QColumn> getSelectedColumns();

    String[] getEntityMappingPath();

    boolean isArrayNode();

    boolean hasArrayDescendantNode();

    boolean isEmpty();

}
