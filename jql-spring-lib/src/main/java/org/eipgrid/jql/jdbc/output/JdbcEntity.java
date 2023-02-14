package org.eipgrid.jql.jdbc.output;

import org.eipgrid.jql.JqlTableController;
import org.eipgrid.jql.JqlTable;
import org.eipgrid.jql.jdbc.JDBCRepositoryBase;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.KVEntity;

import java.util.Map;

class JdbcEntity extends KVEntity implements Map<String, Object> {

    public static class SearchController<ID> extends JqlTableController.Search<ID> {
        public SearchController(JqlTable<ID> store) {
            super(store);
        }
    }

    public static class CRUDController<ID> extends JqlTableController.CRUD<ID> {
        public CRUDController(JqlTable<ID> store) {
            super(store);
        }
    }

    public static class Repository<ID> extends JDBCRepositoryBase<JdbcEntity, ID> {
        public Repository(JdbcStorage storage, QSchema schema) {
            super(storage, schema);
        }
    }
}
