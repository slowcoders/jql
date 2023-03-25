package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.jdbc.JdbcQuery;
import org.eipgrid.jql.parser.JqlFilter;

import java.util.Map;

public interface QueryGenerator {
    String createSelectQuery(JdbcQuery query);

    String createCountQuery(JqlFilter where);

    String createUpdateQuery(JqlFilter where, Map<String, Object> updateSet);

    String createDeleteQuery(JqlFilter where);

    String prepareBatchInsertStatement(JdbcSchema schema, JqlEntitySet.InsertPolicy insertPolicy);

}
