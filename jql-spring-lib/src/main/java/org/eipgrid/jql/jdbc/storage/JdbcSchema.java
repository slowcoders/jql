package org.eipgrid.jql.jdbc.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.jpa.JpaUtils;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.SourceWriter;

import javax.persistence.Column;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import java.lang.reflect.Field;
import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class JdbcSchema extends QSchema {

    private final JdbcSchemaLoader schemaLoader;

    private HashMap<String, ArrayList<String>> uniqueConstraints = new HashMap<>();
    private final HashMap<String, List<QColumn>> fkConstraints = new HashMap<>();

    private Map<String, QJoin> entityJoinMap;
    private ArrayList<QColumn> unresolvedJpaColumns;
    private Class<?> idType;
    private JoinMap importedByFkJoinMap;

    protected JdbcSchema(JdbcSchemaLoader schemaLoader, String tableName, Class<?> ormType) {
        super(tableName, ormType);
        this.schemaLoader = schemaLoader;
    }

    public final JdbcSchemaLoader getStorage() {
        return schemaLoader;
    }


    protected void init(ArrayList<? extends QColumn> columns, HashMap<String, ArrayList<String>> uniqueConstraints, Class<?> ormType) {
        this.uniqueConstraints = uniqueConstraints;

        if (!JqlRepository.RawEntityType.isAssignableFrom(ormType)) {
            HashMap<String, Field> jpaColumns = new HashMap<>();
            for (Field f: JpaUtils.getColumnFields(ormType)) {
                String name = resolvePhysicalName(f);
                jpaColumns.put(name.toLowerCase(), f);
            }

            for (int i = columns.size(); --i >= 0; ) {
                QColumn col = columns.get(i);
                Field f = jpaColumns.get(col.getPhysicalName().toLowerCase());
                if (f == null) {
                    columns.remove(i);
                    if (unresolvedJpaColumns == null) unresolvedJpaColumns = new ArrayList<>();
                    unresolvedJpaColumns.add(col);
                }
                else {
                    super.mapColumn(col, f);
                }
            }
        }
        super.init(columns, ormType);
        this.importedByFkJoinMap = new JoinMap();
        for (List<QColumn> fkColumns : this.fkConstraints.values()) {
            importedByFkJoinMap.add(this, fkColumns, null);
        }
    }

    protected void markAllColumnsToPK(List<QColumn> pkColumns) {
        for (QColumn col : pkColumns) {
            ((JdbcColumn)col).markPrimaryKey();
        }
    }

    public final Map<String, QJoin> getEntityJoinMap() {
        return getEntityJoinMap(true);
    }

    protected Map<String, QJoin> getEntityJoinMap(boolean loadNow) {
        if (loadNow && this.entityJoinMap == null) {
            schemaLoader.loadJoinMap(this);
        }
        return this.entityJoinMap;
    }

    protected void setEntityJoinMap(Map<String, QJoin> joinMap) {
        if (this.entityJoinMap != null) {
            throw new RuntimeException("entityJoinMap is already assigned.");
        }
        this.entityJoinMap = joinMap;
    }

    public static void dumpJPAHeader(SourceWriter sb, boolean includeJsonType) {
        if (includeJsonType) {
            sb.writeln("import com.fasterxml.jackson.databind.JsonNode;");
        }
        sb.writeln("import lombok.Getter;");
        sb.writeln("import lombok.Setter;");
        sb.writeln("import java.util.*;");
        sb.writeln("import javax.persistence.*;");
        sb.writeln();
        sb.writeln("/** This source is generated by JQL-JDBC */\n");
    }

    public void dumpJPAEntitySchema(SourceWriter sb, boolean includeHeader) {
        if (includeHeader) {
            dumpJPAHeader(sb, !this.getObjectColumns().isEmpty());
        }

        String comment = this.schemaLoader.getTableComment(this.getTableName());

        if (comment != null && comment.length() > 0) {
            sb.write("/** ").write(comment).writeln(" */");
        }
        sb.writeln("@Entity");
        dumpTableDefinition(sb);

        /* TODO multi-key, EmbeddedId, Embeddable, Element */
        boolean isMultiPKs = false && getPKColumns().size() > 1;
        String className = this.generateEntityClassName();
        if (isMultiPKs) {
            sb.write("@IdClass(").write(className).writeln(".ID.class)");
        }
        sb.writeln("public class " + className + " implements java.io.Serializable {");
        if (isMultiPKs) {
            sb.incTab();
            sb.write("public static class ID implements Serializable {\n");
            sb.incTab();
            for (QColumn column : getPKColumns()) {
                dumpColumnDefinition(column, sb);
            }
            sb.decTab();
            sb.decTab();
        }
        sb.incTab();
        //idColumns = getIDColumns();
        for (QColumn col : getReadableColumns()) {
            dumpColumnDefinition(col, sb);
            sb.writeln();
        }

        for (Map.Entry<String, QJoin> entry : this.getEntityJoinMap().entrySet()) {
            QJoin join = entry.getValue();
            dumpJoinedColumn(join, sb);

        }
        sb.decTab();
        sb.writeln("}\n");
    }

    private void dumpColumnDefinition(QColumn col, SourceWriter sb) {
        if (col.getJoinedPrimaryColumn() != null) return;

        if (col.getLabel() != null) {
            sb.write("/** ");
            sb.write(col.getLabel());
            sb.writeln(" */");
        }
        boolean isJsonObject = col.isJsonNode();
        if (true || !isJsonObject) {
            sb.write("@Getter");
            if (!col.isReadOnly()) {
                sb.write(" @Setter");
            }
            sb.writeln();
        }

        if (col.isPrimaryKey()) {
            sb.writeln("@Id");
            if (col.isAutoIncrement()) {
                sb.writeln("@GeneratedValue(strategy = GenerationType.IDENTITY)");
            }
        }
        QColumn pk = col.getJoinedPrimaryColumn();
        if (pk != null) {
            boolean isUnique = this.isUniqueConstrainedColumnSet(Collections.singletonList(col));
            sb.write(isUnique ? "@One" : "@Many").writeln("ToOne(fetch = FetchType.LAZY)");
        }
        sb.write(pk != null ? "@Join" : "@").write("Column(name = ").writeQuoted(col.getPhysicalName()).write(", ");
        if (col.isNullable()) sb.write("nullable = true, ");
        if (!col.getValueType().getName().startsWith("java.lang.")) {
            sb.write("columnDefinition = \"").write(((JdbcColumn)col).getColumnTypeName()).write("\", ");
        }
        if (pk != null) {
            sb.write("referencedColumnName = ").writeQuoted(pk.getPhysicalName()).write(", ");
        }
        if (!col.isNullable()) {
            sb.writeln("nullable = false");
        }

        sb.replaceTrailingComma(")\n");

        if (isJsonObject) {
            sb.writeln("@org.hibernate.annotations.Type(type = \"io.hypersistence.utils.hibernate.type.json.JsonType\")");
        }
        String fieldName = getJavaFieldName(col);

//        if (isJsonObject) {
//            sb.write("String ").write(fieldName).writeln(";");
//            fieldName = fieldName + "$";
//            sb.writeln();
//            sb.write("transient ");
//        }
        sb.write("private ").write(getJavaFieldType(col)).write(" ").write(fieldName).writeln(";");

    }

    private void dumpJoinedColumn(QJoin join, SourceWriter sb) {
        boolean isInverseJoin = join.isInverseMapped();
        QColumn firstFk = join.getForeignKeyColumns().get(0);
        if (isInverseJoin && join.getAssociativeJoin() == null && firstFk.getSchema().hasOnlyForeignKeys()) {
            return;
        }

        QSchema mappedSchema = join.getTargetSchema();
        boolean isUniqueJoin = join.hasUniqueTarget();
        boolean isArrayJoin = isInverseJoin && !isUniqueJoin;

        if (!isInverseJoin && join.getForeignKeyColumns().size() == 1) {
            QColumn col = firstFk;
            if (join.getAssociativeJoin() != null && join.getAssociativeJoin().getForeignKeyColumns().size() == 1) {
                col = join.getAssociativeJoin().getForeignKeyColumns().get(0);
            }
            if (col.getLabel() != null) {
                sb.write("/** ");
                sb.write(col.getLabel());
                sb.writeln(" */");
            }
        }

        sb.write("@Getter @Setter\n");

        if (!isInverseJoin && firstFk.isPrimaryKey()) {
            sb.write("@Id\n");
        }

        sb.write('@').write(join.getType().toString()).write("(fetch = FetchType.LAZY");
        if (isInverseJoin && join.getAssociativeJoin() == null) {
            String mappedField = getJavaFieldName(firstFk);
            sb.write(", mappedBy = ").writeQuoted(mappedField);
        }
        sb.write(")\n");

        if (!isInverseJoin) {
            QColumn fk = firstFk;
            sb.write("@JoinColumn(name = ").writeQuoted(fk.getPhysicalName()).write(", ");
            if (fk.isNullable()) sb.write("nullable = true, ");
            sb.write("referencedColumnName = ").writeQuoted(fk.getJoinedPrimaryColumn().getPhysicalName()).write(", \n");
            sb.incTab();
            sb.write("foreignKey = @ForeignKey(name = ").writeQuoted(this.getFKConstraintName(fk)).write("))\n");
            sb.decTab();
        }
        else if (join.getAssociativeJoin() != null) {
            sb.write("@JoinTable(name = ").writeQuoted(join.getLinkedSchema().getSimpleName()).write(", ");
            String namespace = join.getLinkedSchema().getNamespace();
            if (namespace != null) {
                sb.write("schema = ").writeQuoted(namespace).write(", ");
                sb.write("catalog = ").writeQuoted(namespace).write(",");
            }
            sb.writeln();
            sb.incTab();
            if (firstFk.getSchema().hasOnlyForeignKeys()) {
                ((JdbcSchema)firstFk.getSchema()).dumpUniqueConstraints(sb);
            }
            sb.write("joinColumns = @JoinColumn(name=").writeQuoted(firstFk.getPhysicalName()).write("), ");
            sb.write("inverseJoinColumns = @JoinColumn(name=").writeQuoted(join.getAssociativeJoin().getForeignKeyColumns().get(0).getPhysicalName()).write("))\n");
            sb.decTab();
        }

        String mappedType = ((JdbcSchema)mappedSchema).generateEntityClassName();
        sb.write("private ");
        if (!isArrayJoin) {
            sb.write(mappedType);
        } else {
            JdbcSchema fkSchema = (JdbcSchema)firstFk.getSchema();
            /* List ?????? ?????? ?????? ????????? ???????????? Hibernate ??? MultiBag ????????? ???????????????.
               Set ??? ???????????? ?????????, ?????? Data ??? ???????????? ????????? ????????????.
               2022.02.17
               ???????????? List ??? ????????? ???????????? ?????? ????????? ???????????? ?????????. ?????? ?????? ?????? Array ??? Set ?????? ??????.
             */
            boolean partOfUnique = true || fkSchema.hasGeneratedId() || fkSchema.uniqueConstraints.size() > 0;
            sb.write(partOfUnique ? "Set<" : "List<").write(mappedType).write(">");
        }
        sb.write(" ").write(getJavaFieldName(join)).write(";\n\n");
    }

    private String getFKConstraintName(QColumn fk) {
        for (Map.Entry<String, List<QColumn>> constraint : fkConstraints.entrySet()) {
            List<QColumn> cols = constraint.getValue();
            if (cols.contains(fk)) {
                return constraint.getKey();
            }
        }
        throw new RuntimeException("fkConstraint not found");
    }


    private void dumpTableDefinition(SourceWriter sb) {
        sb.write("@Table(name = ").writeQuoted(this.getSimpleName()).write(", ");
        String namespace = this.getNamespace();
        if (namespace != null) {
            sb.write("schema = ").writeQuoted(namespace).write(", ");
            sb.write("catalog = ").writeQuoted(namespace).write(",");
        }
        sb.writeln();
        sb.incTab();
        dumpUniqueConstraints(sb);
        sb.decTab();
        sb.replaceTrailingComma("\n)\n");
    }

    private void dumpUniqueConstraints(SourceWriter sb) {
        if (!this.uniqueConstraints.isEmpty()) {
            sb.write("uniqueConstraints = {");
            sb.incTab();
            for (Map.Entry<String, ArrayList<String>> entry: this.uniqueConstraints.entrySet()) {
                sb.write("\n@UniqueConstraint(name =\"" + entry.getKey() + "\", columnNames = {");
                sb.incTab();
                for (String column : entry.getValue()) {
                    sb.writeQuoted(column).write(", ");
                }
                sb.replaceTrailingComma("}),");
                sb.decTab();
            }
            sb.decTab();
            sb.replaceTrailingComma("\n},\n");
        }
    }

    private String getJavaFieldName(QJoin join) {
        String name = join.getJsonKey();
        if (name.charAt(0) == '+') {
            name = name.substring(1);
        }
        return name;
    }



    private String getJavaFieldType(QColumn col) {
        String name = col.getValueType().getName();
        if (name.startsWith("java.lang.")) {
            name = name.substring(10);
        }
        return name;
    }


    public boolean isUniqueConstrainedColumnSet(List<QColumn> fkColumns) {
        int cntColumn = fkColumns.size();
        compare_constraint:
        for (List<String> uc : this.uniqueConstraints.values()) {
            if (uc.size() != cntColumn) continue;
            for (QColumn col : fkColumns) {
                String col_name = col.getPhysicalName();
                if (!uc.contains(col_name)) {
                    continue compare_constraint;
                }
            }
            return true;
        }
        return false;
    }

    protected QJoin getJoinByForeignKeyConstraints(String fkConstraint) {
        List<QColumn> fkColumns = this.fkConstraints.get(fkConstraint);
        for (QJoin join : this.importedByFkJoinMap.values()) {
            if (join.getForeignKeyColumns() == fkColumns) {
                assert(join.getBaseSchema() == this && !join.isInverseMapped());
                return join;
            }
        }
        for (QJoin join : this.importedByFkJoinMap.values()) {
            QSchema schema = join.getLinkedSchema();// getForeignKeyColumns().get(0).getSchema();
            System.out.println(schema.getTableName());
        }
        throw new Error("fk join not found: " + fkConstraint);
    }

    HashMap<String, List<QColumn>> getForeignKeyConstraints() {
        return this.fkConstraints;
    }
    protected void addForeignKeyConstraint(String fk_name, JdbcColumn fkColumn) {
        List<QColumn> fkColumns = fkConstraints.get(fk_name);
        if (fkColumns == null) {
            fkColumns = new ArrayList<>();
            fkColumns.add(fkColumn);
            fkConstraints.put(fk_name, fkColumns);
        } else {
            fkColumns.add(fkColumn);
        }
    }

    private String resolvePhysicalName(Field f) {
        if (true) {
            Column c = f.getAnnotation(Column.class);
            if (c != null) {
                String colName = c.name();
                if (colName != null && colName.length() > 0) {
                    return colName;
                }
            }
        }
        if (true) {
            JoinColumn c = f.getAnnotation(JoinColumn.class);
            if (c != null) {
                String colName = c.name();
                if (colName != null && colName.length() > 0) {
                    return colName;
                }
            }
        }
        String colName = schemaLoader.toPhysicalColumnName(f.getName());
        return colName;
    }

    public Class<?> getIdType() {
        if (this.idType == null) {
            List<QColumn> pkColumns = this.getPKColumns();
            if (pkColumns.size() == 1) {
                this.idType = pkColumns.get(0).getValueType();
            }
            else if (!isJPARequired()) {
                this.idType = JdbcArrayID.class;
            }
            else {
                IdClass idClass = getEntityType().getAnnotation(IdClass.class);
                this.idType = idClass.value();
            }
        }
        return this.idType;
    }
    public <ID, ENTITY> ID getEnityId(ENTITY entity) {
        if (entity == null) return null;
        if (!getEntityType().isAssignableFrom(entity.getClass())) {
            throw new RuntimeException("Entity type mismatch: " +
                    getEntityType().getSimpleName() + " != " + entity.getClass().getSimpleName());
        }

        try {
            List<QColumn> pkColumns = this.getPKColumns();
            if (pkColumns.size() == 1) {
                QColumn pk = pkColumns.get(0);
                if (this.isJPARequired()) {
                    Field f = pk.getMappedOrmField();
                    f.setAccessible(true);
                    return (ID)f.get(entity);
                } else {
                    return (ID)((Map)entity).get(pk.getJsonKey());
                }
            }
            else {
                if (this.isJPARequired()) {
                    Object id = getIdType().getConstructor().newInstance();
                    for (QColumn column : pkColumns) {
                        Object k = column.getMappedOrmField().get(entity);
                        column.getMappedOrmField().set(id, k);
                    }
                    return (ID)id;
                }
                else {
                    Object[] keys = new Object[pkColumns.size()];
                    int i = 0;
                    for (QColumn column : pkColumns) {
                        keys[i++] = ((Map)entity).get(column.getJsonKey());
                    }
                    return (ID)new JdbcArrayID(keys);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*package*/ final Map<String, QJoin> getEntityJoinMap_unsafe() {
        return this.entityJoinMap;
    }

    /*packet*/ final JoinMap getImportedJoins() {
        return importedByFkJoinMap;
    }
}
