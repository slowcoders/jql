package org.eipgrid.jql.parser;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

public class JsonColumn extends QColumn {
    protected JsonColumn(String name, Class type) {
        super(name, type);
    }

    @Override
    public QSchema getSchema() { return null; }

    @Override
    public String getJsonKey() {
        return super.getPhysicalName();
    }
}
