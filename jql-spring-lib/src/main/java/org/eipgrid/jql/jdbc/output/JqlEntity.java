package org.eipgrid.jql.jdbc.output;

import org.eipgrid.jql.JqlTableController;
import org.eipgrid.jql.JqlTable;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.jdbc.JDBCRepositoryBase;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.KVEntity;

import java.util.Map;

class JqlEntity extends KVEntity implements Map<String, Object> {

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

    public static class Repository<ID> extends JDBCRepositoryBase<JqlEntity, ID> {
        public Repository(JqlStorage storage, QSchema schema) {
            super(storage, schema);
        }
    }
}
