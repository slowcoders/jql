package org.eipgrid.jql.schema;

import org.eipgrid.jql.jpa.JpaUtils;
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
            return resolveForeignKeyPropertyName(first_fk);
        }

        String name;
        if (inverseMapped) {
            Class<?> jpaType = baseSchema.getEntityType();
            Class<?> jpaClass = getTargetSchema().getEntityType();
            if (jpaType.getAnnotation(Entity.class) != null) {
                for (Field f : JpaUtils.getCacheableFields(jpaType)) {
                    Class<?> itemT = ClassUtils.getElementType(f);
                    if (jpaClass == itemT) {
                        // TODO MappedBy 검사 필요(?)
                        return f.getName();
                    };
                }
            }
            // column 이 없으므로 타입을 이용하여 이름을 정한다.
            name = first_fk.getSchema().getSimpleName();
        }
        else {
            name = first_fk.getJoinedPrimaryColumn().getSchema().getSimpleName();
        }

        name = baseSchema.getStorage().toEntityClassName(name, false);
        return name;
    }

    public static String resolveForeignKeyPropertyName(QColumn fk) {
        if (fk.getMappedOrmField() != null) {
            return fk.getJsonKey();
        }

        QColumn joinedPk = fk.getJoinedPrimaryColumn();
        String fk_name = fk.getPhysicalName().toLowerCase();
        String pk_name = joinedPk.getPhysicalName().toLowerCase();
        String js_key;
        int p = fk_name.lastIndexOf('_') + 1;
        if (p <= 0 || p == fk_name.length()) {
            js_key = fk_name;
        }
        else {
            String suffix = fk_name.substring(p);
            if (pk_name.endsWith(suffix) || suffix.equals("id")) { //  && fk.getSchema().findColumn(base = fk_name.substring(0, p - 1)) == null) {
                js_key = fk_name.substring(0, p - 1); // base;
            } else {
                js_key = fk_name;
            }
        }
        QSchema fk_schema = fk.getSchema();
        js_key = fk_schema.getStorage().toLogicalAttributeName(fk_schema.getSimpleName(), js_key);
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
