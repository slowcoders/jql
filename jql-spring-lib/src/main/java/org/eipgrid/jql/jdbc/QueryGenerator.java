package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.parser.JqlFilter;

import java.util.Map;

public interface QueryGenerator {
    String createSelectQuery(JqlQuery query);

    String createCountQuery(JqlFilter where);

    String createUpdateQuery(JqlFilter where, Map<String, Object> updateSet);

    String createDeleteQuery(JqlFilter where);

}
