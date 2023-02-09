package org.eipgrid.jql.schema;

import org.eipgrid.jql.util.CaseConverter;
import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.Entity;
import java.lang.reflect.Field;
import java.util.List;

public class QJoin {

    private final Type type;
    private final QJoin associateJoin;
    private String jsonKey;
    private final boolean inverseMapped;
    private final List<QColumn> fkColumns;
    private final QSchema baseSchema;

    public enum Type {
        OneToOne,
        ManyToOne,
        OneToMany,
        ManyToMany
    }

    public QJoin(QSchema baseSchema, List<QColumn> fkColumns) {
        this(baseSchema, fkColumns, null);
    }

    public QJoin(QSchema baseSchema, List<QColumn> fkColumns, QJoin associateJoin) {
        this.fkColumns = fkColumns;
        this.baseSchema = baseSchema;
        this.associateJoin = associateJoin;
        QSchema fkSchema = fkColumns.get(0).getSchema();
        this.inverseMapped = baseSchema != fkSchema;
        boolean uniqueBase;
        boolean uniqueTarget;
        if (inverseMapped) { // PK:FK  mapping (OneToOne or OneToMany) + (ManyToMany:associative)
            assert(associateJoin == null || !associateJoin.inverseMapped);
            uniqueBase = (associateJoin == null || fkSchema.isUniqueConstrainedColumnSet(fkColumns));
            uniqueTarget = (associateJoin == null || associateJoin.hasUniqueTarget()) &&
                            fkSchema.isUniqueConstrainedColumnSet(fkColumns);
        } else { // FK:PK  mapping (OneToOne or ManyToOne)
            assert(associateJoin == null);
            uniqueBase = fkSchema.isUniqueConstrainedColumnSet(fkColumns);
            uniqueTarget = true;
        }
        if (uniqueTarget) {
            this.type = uniqueBase ? Type.OneToOne : Type.ManyToOne;
        } else {
            this.type = uniqueBase ? Type.OneToMany : Type.ManyToMany;
        }
        String key = associateJoin != null ? associateJoin.getJsonKey() : resolveJsonKey();
        if (!hasUniqueTarget() && !key.endsWith("_")) {
            key += '_';
        }
        this.jsonKey = key;
    }

    private boolean isRecursiveJoin() {
        return associateJoin != null &&
                baseSchema == associateJoin.fkColumns.get(0).getJoinedPrimaryColumn().getSchema();
    }

    public void resolveNameConflict(QJoin old) {
        String old_name = old.jsonKey;
        int pos = old_name.lastIndexOf('$');
        if (pos > 0) {
            try {
                int no = Integer.parseInt(old_name.substring(pos + 1));
                this.jsonKey = old_name.substring(0, pos + 1) + (no + 1);
                return;
            } catch (Exception e) {
                // ignore
            }
        }
        this.jsonKey = old_name + "_2";
    }


    public List<QColumn> getForeignKeyColumns() {
        return fkColumns;
    }

    public QJoin getAssociativeJoin() {
        return associateJoin;
    }

    public boolean isInverseMapped() {
        return this.inverseMapped;
    }

    public boolean hasUniqueTarget() {
        return this.type.ordinal() < Type.OneToMany.ordinal();
    }

    public Type getType() {
        return this.type;
    }
    public String getJsonKey() {
        return jsonKey;
    }

    private String resolveJsonKey() {
        if (this.jsonKey != null) {
            throw new RuntimeException("already initialized");
        }
        QColumn first_fk = fkColumns.get(0);
        if (!inverseMapped && fkColumns.size() == 1) {
            return resolveJsonKey(first_fk);
        }

        String name;
        if (inverseMapped) {
            Class<?> jpaType = baseSchema.getEntityType();
            Class<?> jpaClass = getTargetSchema().getEntityType();
            if (jpaType.getAnnotation(Entity.class) != null) {
                for (Field f : ClassUtils.getInstanceFields(jpaType, true)) {
                    Class<?> itemT = ClassUtils.getElementType(f);
                    if (jpaClass == itemT) {
                        // TODO MappedBy 검사 필요(?)
                        return f.getName();
                    };
                }
            }
            // column 이 없으므로 타입을 이용하여 이름을 정한다.
            name = first_fk.getSchema().getSimpleTableName();
        }
        else {
            name = first_fk.getJoinedPrimaryColumn().getSchema().getSimpleTableName();
        }

        name = CaseConverter.toCamelCase(name, false);
        return name;
    }

    public static String resolveJsonKey(QColumn fk) {
        if (fk.getMappedOrmField() != null) {
            return fk.getJsonKey();
        }

        String fk_name = fk.getPhysicalName();
        QColumn joinedPk = fk.getJoinedPrimaryColumn();
        String pk_name = joinedPk.getPhysicalName();
        String js_key;
        if (fk_name.endsWith("_" + pk_name)) {
            js_key = CaseConverter.toCamelCase(fk_name.substring(0, fk_name.length() - pk_name.length() - 1), false);
        } else {
            js_key = joinedPk.getSchema().getSimpleTableName();
        }
        return js_key;
    }

    public QSchema getBaseSchema() {
        return this.baseSchema;
    }

    public QSchema getLinkedSchema() {
        QColumn col = fkColumns.get(0);
        if (!inverseMapped) {
            col = col.getJoinedPrimaryColumn();
        }
        return col.getSchema();
    }

    public QSchema getTargetSchema() {
        if (associateJoin != null) {
            return associateJoin.getLinkedSchema();
        } else {
            return this.getLinkedSchema();
        }
    }
}
