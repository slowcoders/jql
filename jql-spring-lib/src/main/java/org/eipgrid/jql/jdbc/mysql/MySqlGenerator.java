package org.eipgrid.jql.jdbc.mysql;

import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.jdbc.storage.JdbcSchema;
import org.eipgrid.jql.jdbc.storage.SqlGenerator;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

import java.util.Map;

public class MySqlGenerator extends SqlGenerator {
    public MySqlGenerator(boolean isNativeQuery) {
        super(isNativeQuery);
    }

    private void writeInsertHeader(QSchema schema, JqlEntitySet.InsertPolicy insertPolicy) {
        sw.writeln();
        sw.write("INSERT ");
        if (insertPolicy == JqlEntitySet.InsertPolicy.IgnoreOnConflict) sw.write("IGNORE ");
        sw.write("INTO ").write(schema.getTableName());
    }

    public String createInsertStatement(QSchema schema, Map<String, Object> entity, JqlEntitySet.InsertPolicy insertPolicy) {
        this.writeInsertHeader(schema, insertPolicy);

        super.writeInsertStatementInternal(schema, entity);

        switch (insertPolicy) {
            case IgnoreOnConflict:
                sw.write("\nON CONFLICT DO NOTHING");
                break;
            case UpdateOnConflict:
                if (!schema.hasGeneratedId()) {
                    sw.write("\nON CONFLICT DO UPDATE");
                    for (Map.Entry<String, Object> entry : entity.entrySet()) {
                        String col = schema.getColumn(entry.getKey()).getPhysicalName();
                        sw.write("  ");
                        sw.write(col).write(" = VALUES(").write(col).write("),\n");
                    }
                    sw.replaceTrailingComma("\n");
                }
        }
        String sql = sw.reset();
        return sql;
    }


    public String prepareBatchInsertStatement(JdbcSchema schema, JqlEntitySet.InsertPolicy insertPolicy) {
        this.writeInsertHeader(schema, insertPolicy);

        super.writePreparedInsertStatementValueSet(schema.getWritableColumns());

        switch (insertPolicy) {
            case UpdateOnConflict:
                if (!schema.hasGeneratedId()) {
                    sw.write("\nON DUPLICATE KEY UPDATE\n"); // SET 포함 안 함? 아래 문장 하나로 해결??
                    for (QColumn column : schema.getWritableColumns()) {
                        String col = column.getPhysicalName();
                        sw.write("  ");
                        sw.write(col).write(" = VALUES(").write(col).write("),\n");
                    }
                    sw.replaceTrailingComma("\n");
                }
        }
        String sql = sw.reset();
        return sql;
    }

}
