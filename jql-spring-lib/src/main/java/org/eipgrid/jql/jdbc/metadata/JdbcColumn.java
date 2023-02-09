package org.eipgrid.jql.jdbc.metadata;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.schema.QType;
import org.eipgrid.jql.util.ClassUtils;

import java.lang.reflect.Field;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class JdbcColumn extends QColumn {

    private final boolean isReadOnly;
    private final boolean isAutoIncrement;
    private final boolean isNullable;
    private final boolean isPk;
    private Field field;

    private String fieldName;

    private String label;
    private ColumnBinder fkBinder;

    //*
    private String colTypeName;

    private int displaySize;
    private int precision;
    private int scale;
    //*/

    public JdbcColumn(QSchema schema, ResultSetMetaData md, int col, ColumnBinder fkBinder, String comment, ArrayList<String> primaryKeys) throws SQLException {
        super(schema, md.getColumnName(col), resolveJavaType(md, col));

        this.isAutoIncrement = md.isAutoIncrement(col);
        this.isReadOnly = md.isReadOnly(col) | this.isAutoIncrement;
        this.isNullable = md.isNullable(col) != ResultSetMetaData.columnNoNulls;
        this.isPk = primaryKeys.contains(this.getPhysicalName());

        this.fkBinder = fkBinder;
        this.field = null;
        boolean isWritable = md.isWritable(col);
        if (!isWritable) {
            throw new RuntimeException("!isWritable");
        }
        this.colTypeName = md.getColumnTypeName(col);
        this.label = comment != null ? comment : md.getColumnLabel(col);
        this.precision = md.getPrecision(col);
        this.scale = md.getScale(col);
        this.displaySize = md.getColumnDisplaySize(col);
    }

//    public JdbcColumn(QSchema schema, Field f) {
//        super(schema, resolveColumnName(schema, f), ClassUtils.getElementType(f), QType.of(f));
//        super.setMappedField(f);
////        this.fk = resolveForeignKey(f);
//
//        this.field = f;
//
//        GeneratedValue gv = f.getAnnotation(GeneratedValue.class);
//        this.isAutoIncrement = gv != null && gv.strategy() == GenerationType.IDENTITY;
//        this.isPk = JPAUtils.isIdField(f);
//
////        this.isNullable = resolveNullable(f);
//        this.isReadOnly = gv != null;
//        this.label = null;
//    }

    public boolean isForeignKey() { return fkBinder != null; }

    public Field getMappedOrmField() { return this.field; }

    protected void setMappedOrmField(Field f) {
        this.field = f;
        this.fieldName = f.getName();
    }

    @Override
    public String getJsonKey() {
        if (fieldName == null) {
            fieldName = this.resolveFieldName();
        }
        return fieldName;
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public boolean isNullable() {
        return this.isNullable;
    }

    @Override
    public boolean isAutoIncrement() {
        return this.isAutoIncrement;
    }

    @Override
    public boolean isPrimaryKey() { return this.isPk; }

    @Override
    public String getLabel() {
        return this.label;
    }

    public String getDBColumnType() {
        return colTypeName;
    }

    public QColumn getJoinedPrimaryColumn() {
        if (this.fkBinder == null) return null;
        return fkBinder.getJoinedColumn();
    }

    private String resolveFieldName() {
        StringBuilder sb = new StringBuilder();
        QColumn col = this;
        for (QColumn joinedPk; (joinedPk = col.getJoinedPrimaryColumn()) != null; col = joinedPk) {
            String token = QJoin.resolveJsonKey(col);
            sb.append(token).append('.');
        }
        CharSequence rawFieldName = (col != this) ? sb.append(col.getPhysicalName()) : this.getPhysicalName();

        String name = getSchema().getSchemaLoader().getNameConverter().toLogicalAttributeName(rawFieldName.toString());
        return name;
    }


    protected void bindPrimaryKey(ColumnBinder pkBinder) {
        this.fkBinder = pkBinder;
    }

    private static Class resolveJavaType(ResultSetMetaData md, int col) throws SQLException {
        String colTypeName = md.getColumnTypeName(col).toLowerCase();
        int colType = md.getColumnType(col);
        try {
            switch (colTypeName) {
                case "json":
                case "jsonb":
                    return Map.class;
                default:
                    String javaClassName = md.getColumnClassName(col);
                    return ClassUtils.getBoxedType(Class.forName(javaClassName));
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public String getStoredType() {
        return colTypeName;
    }
}
