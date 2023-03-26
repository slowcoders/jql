package org.eipgrid.jql.jdbc.mariadb;

import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.jdbc.mysql.MySqlGenerator;
import org.eipgrid.jql.jdbc.storage.JdbcSchema;
import org.eipgrid.jql.jdbc.storage.SqlGenerator;
import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.parser.EntityFilter;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

import java.util.List;
import java.util.Map;

public class MariaSqlGenerator extends MySqlGenerator {
    public MariaSqlGenerator(boolean isNativeQuery) {
        super(isNativeQuery);
    }

    protected void writeJsonPath(EntityFilter node, QColumn column, JsType valueType) {
        sw.write("json_value(");
        writeJsonPath(node);
        sw.write(column.getJsonKey()).write('\'');
        sw.write(')');
    }

    private void writeJsonPath(EntityFilter node) {
        if (node.isJsonNode()) {
            EntityFilter parent = node.getParentNode();
            writeJsonPath(parent);
            if (!parent.isJsonNode()) {
                sw.write("`").write(node.getMappingAlias()).write("`, '$");
            } else {
                sw.write(node.getMappingAlias());
            }
        } else {
            sw.write(node.getMappingAlias());
        }
        sw.write('.');
    }
}
