package org.eipgrid.jql.js;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.jdbc.storage.JdbcColumn;
import org.eipgrid.jql.util.CaseConverter;
import org.eipgrid.jql.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsUtil {

    public static String createDDL(QSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("const " + schema.getSimpleName() + "Schema_columns = [\n");
        for (QColumn col : schema.getLeafColumns()) {
            dumpJSONSchema(sb, (JdbcColumn)col);
            sb.append(",\n");
        }
        sb.append("];\n");

        if (!schema.getEntityJoinMap().isEmpty() || !schema.getObjectColumns().isEmpty()) {
            sb.append("\nconst " + schema.getSimpleName() + "Schema_external_entities = [\n");
            for (QColumn column : schema.getObjectColumns()) {
                sb.append("  jql.externalJoin(\"").append(column.getJsonKey()).append("\", Object,\n");
            }
            for (Map.Entry<String, QJoin> entry : schema.getEntityJoinMap().entrySet()) {
                sb.append("  jql.externalJoin(\"").append(entry.getKey()).append("\", ");
                sb.append(entry.getValue().getLinkedSchema().getSimpleName()).append("Schema, ");
                sb.append(entry.getValue().hasUniqueTarget() ? "Object" : "Array").append("),\n");
            }
            sb.append("];\n");
        }

        return sb.toString();
    }

    public static String dumpJSONSchema(StringBuilder sb, JdbcColumn col) {
        String jsonType = getColumnType(col);
        if (jsonType == null) {
            throw new RuntimeException("JsonType not registered: " + col.getValueType() + " " + col.getPhysicalName());
        }
        sb.append("  jql.").append(jsonType).append("(\"")
                .append(col.getJsonKey()).append("\"");
        sb.append(")");

        if (col.isReadOnly()) {
            sb.append(".readOnly()");
        }
        else if (!col.isNullable()) {
            sb.append(".required()");
        }

        return sb.toString();
    }



    public static String createJoinJQL(QSchema schema) {
        StringBuilder sb = new StringBuilder();
        for (QColumn jqlColumn : schema.getReadableColumns()) {
            QColumn joined_pk = jqlColumn.getJoinedPrimaryColumn();
            if (joined_pk == null) continue;

            sb.append(schema.getSimpleName()).append("Schema.join(\"");
            sb.append(jqlColumn.getJsonKey()).append("\", ");
            sb.append(joined_pk.getSchema().getEntityType().getSimpleName()).append("Schema, \"");
            sb.append(joined_pk.getJsonKey()).append("\");\n");
        }
        return sb.toString();
    }

//    private static String getJoinName(String name) {
//        int i = name.lastIndexOf('_');
//        while (i > 0) {
//            if (name.charAt(--i) != '_') {
//                name = name.substring(0, i+1);
//                break;
//            }
//        }
//        return name;
//    }
//
    private static String getColumnType(QColumn column) {
        QColumn joinedPK = column.getJoinedPrimaryColumn();
        if (joinedPK != null) {
            String columnType = CaseConverter.toCamelCase(joinedPK.getSchema().getSimpleName(), true);
            return columnType;
        }

        Class javaType = column.getValueType();
        String type = ClassUtils.getBoxedType(javaType).getName();
        String colType = mdkTypes.get(type);
        if (colType == null) {
            colType = "Object";
            //throw new RuntimeException(type + " is not registered");
        }
        return colType;
    }

    private static void dumpColumnInfo(QColumn col, StringBuilder sb) {
        QColumn joinedPK = col.getJoinedPrimaryColumn();
        String columnType = getColumnType(col);
        if (!col.isNullable()) {
            columnType = columnType + '!';
        }
        sb.append(columnType).append(filler.substring(columnType.length()));
        sb.append(col.getJsonKey()).append('(').append(col.getPhysicalName()).append(')');
        if (col.isPrimaryKey()) {
            sb.append(" PK");
        }
        if (joinedPK != null) {
            sb.append(" FK -> ");
            sb.append(joinedPK.getSchema().getTableName());
            sb.append(".").append(joinedPK.getJsonKey());
        }

        sb.append("\n");
    }
    static String filler = "                ";
    public static String getSimpleSchema(QSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type").append(filler.substring("Type".length())).append("Key(physical_column_name)\n");
        sb.append("--------------------------------------------------\n");
        List<QColumn> primitiveColumns = schema.getLeafColumns();
        for (QColumn col : primitiveColumns) {
            dumpColumnInfo(col, sb);
        }

        boolean hasRef = primitiveColumns.size() != schema.getReadableColumns().size()
                || !schema.getEntityJoinMap().isEmpty();

        if (hasRef) {
            sb.append("\n// reference properties //\n");

            for (QColumn col : schema.getReadableColumns()) {
                if (!JsType.of(col.getValueType()).isPrimitive()) {
                    dumpColumnInfo(col, sb);
                }
            }

            for (Map.Entry<String, QJoin> entry : schema.getEntityJoinMap().entrySet()) {
                QJoin join = entry.getValue();
                QSchema refSchema = join.getTargetSchema();
                if (refSchema.hasOnlyForeignKeys()) continue;
                int start = sb.length();
                QSchema targetSchema = join.getTargetSchema();
                sb.append(join.getTargetSchema().generateEntityClassName());
                if (!join.hasUniqueTarget()) sb.append("[]");
                int type_len = sb.length() - start;
                if (type_len >= filler.length()) {
                    type_len = filler.length() - 1;
                }
                sb.append(filler.substring(type_len));
                sb.append(entry.getKey());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static final HashMap<String, String> mdkTypes = new HashMap<>();
    static {
        mdkTypes.put("java.lang.String", "Text");
        mdkTypes.put("java.lang.Character", "Text");

        mdkTypes.put("java.lang.Boolean", "Boolean");

        mdkTypes.put("java.lang.Byte", "Number");
        mdkTypes.put("java.lang.Short", "Number");
        mdkTypes.put("java.lang.Integer", "Number");
        mdkTypes.put("java.lang.Long", "Number");
        mdkTypes.put("java.lang.Float", "Number");
        mdkTypes.put("java.lang.Double", "Number");
        mdkTypes.put("java.math.BigDecimal", "Number");

        /*
         date 형은 max(13) : 날짜만.
         time 형은 max(15) : 시간만.
         timestamp 형은 max(29)
         timestamptz 형은 max(35)
         */
        mdkTypes.put("java.sql.Date", "Date");
        mdkTypes.put("java.sql.Time", "Time");
        mdkTypes.put("java.sql.Timestamp", "Timestamp");
        mdkTypes.put("org.postgresql.util.PGInterval", "TimeInterval");

        mdkTypes.put("java.util.Map", "Object");
        mdkTypes.put("java.util.Map", "Object");
        mdkTypes.put("Array", "Array");

        /**
         * https://www.postgresql.org/docs/current/datatype.html
         */
        mdkTypes.put("json", "EmbeddedObject");
        mdkTypes.put("jsonb", "EmbeddedObject");
        mdkTypes.put("Array", "Array");

        if (false) {
            mdkTypes.put("UUID", "UUID");
            mdkTypes.put("hstore", "Text");
            mdkTypes.put("box", "Box");
            mdkTypes.put("lseg", "Array");
            mdkTypes.put("point", "Point");
            mdkTypes.put("polygon", "Polygon");
            mdkTypes.put("inet", "Inet4"); // IP4 Address
            mdkTypes.put("macaddr", "Text"); // mac Address
        }
    }

}
