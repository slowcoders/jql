package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.KVEntity;

public class JdbcRepositoryImpl<ID> extends JdbcRepositoryBase<ID> {

    protected JdbcRepositoryImpl(JdbcStorage storage, QSchema schema) {
        super(storage, schema);
    }

}
