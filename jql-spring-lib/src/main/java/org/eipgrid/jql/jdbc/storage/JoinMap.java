package org.eipgrid.jql.jdbc.storage;

import org.eipgrid.jql.jpa.JpaUtils;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.*;

class JoinMap extends HashMap<String, QJoin> {

    private final JdbcSchema baseSchema;

    public JoinMap(JdbcSchema baseSchema) {
        this.baseSchema = baseSchema;
    }

    /*internal*/ QJoin addExportedJoin(ArrayList<Field> mappedFields, JoinConstraint fkColumns, QJoin associateJoin) {
        // FK -> baseSchema.PK
        assert (baseSchema == fkColumns.resolvePkSchema());
        QJoin join = new QJoin(baseSchema, fkColumns, associateJoin);
        if (baseSchema.isJPARequired()) {
            String name = getMappedFieldName(mappedFields, fkColumns, associateJoin);
            if (name != null) {
                join.setJsonKey_unsafe(name);
            }
        }

        QJoin old = super.get(join.getJsonKey());
        if (old != null) {
            join.resolveNameConflict(old);
        }
        old = this.put(join.getJsonKey(), join);
        assert (old == null);
        return join;
    }

    public QJoin addImportedJoin(JoinConstraint fkColumns) {
        // baseSchema.FK -> PK
        assert (baseSchema == fkColumns.getFkSchema());
        QJoin join = new QJoin(baseSchema, fkColumns, null);
        QJoin old = this.put(join.getJsonKey(), join);
        assert (old == null);
        return join;
    }

    private String getMappedFieldName(ArrayList<Field> mappedFields, JoinConstraint p2fJoin, QJoin f2pJoin) {
        QSchema targetSchema;
        JoinConstraint rightConstraint = null;
        if (f2pJoin == null) {
            targetSchema = p2fJoin.getFkSchema();
            rightConstraint = p2fJoin;
        }
        else {
            targetSchema = f2pJoin.getTargetSchema();
            rightConstraint = f2pJoin.getJoinConstraint();
        }
        Class componentType = targetSchema.getEntityType();

        for (Field f : mappedFields) {
            if (ClassUtils.getElementType(f) == componentType) {
                if (isMappedBy(targetSchema, f, rightConstraint)) {
                    return f.getName();
                }
            }
        }
        return null;
    }

    private boolean isMappedBy(QSchema targetSchema, Field f, JoinConstraint joinConstraint) {
        String name;
        OneToMany oneToMany = f.getAnnotation(OneToMany.class);
        if (oneToMany != null && (name = oneToMany.mappedBy()).length() > 0) {
            return targetSchema.hasProperty(name);
        }
        OneToOne oneToOne = f.getAnnotation(OneToOne.class);
        if (oneToOne != null && (name = oneToOne.mappedBy()).length() > 0) {
            return targetSchema.hasProperty(name);
        }
        ManyToMany manyToMany = f.getAnnotation(ManyToMany.class);
        if (manyToMany != null && (name = manyToMany.mappedBy()).length() > 0) {
            return targetSchema.hasProperty(name);
        }
        JoinTable joinTable = f.getAnnotation(JoinTable.class);
        if (joinTable == null) return false;

        JoinColumn[] inverseColumns = joinTable.inverseJoinColumns();
        if (inverseColumns.length == joinConstraint.size()) {
            for (JoinColumn col : inverseColumns) {
                if (joinConstraint.getColumnByPhysicalName(col.name()) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    static class Builder extends HashMap<QSchema, QJoin> {
        private String tableName;
        private HashSet<QSchema> conflictMappings = new HashSet<>();
        public Builder(JdbcSchema pkSchema) {
            this.tableName = pkSchema.generateEntityClassName().toLowerCase();
        }

        public QJoin put(QSchema schema, QJoin childJoin) {
            QJoin oldJoin = super.put(schema, childJoin);
            if (oldJoin != null && oldJoin != childJoin) {
                if (oldJoin.getJsonKey().toLowerCase().startsWith(tableName)) {
                    super.put(schema, oldJoin);
                    return oldJoin;
                } else if (childJoin.getJsonKey().toLowerCase().equals(tableName)) {
                    // do nothing;
                    return oldJoin;
                }
                conflictMappings.add(schema);
            }
            return oldJoin;
        }

        public Map<String, QJoin> createJoinMap(JdbcSchema baseSchema) {
            for (QSchema schema : conflictMappings) {
                super.remove(schema);
            }

            ArrayList<Field> mappedFields = JpaUtils.getMappedFields(baseSchema.getEntityType());
            JoinMap joinMap = new JoinMap(baseSchema);
            joinMap.putAll(baseSchema.getImportedJoins());

            for (QJoin fkJoin : this.values()) {
                JoinConstraint fkColumns = fkJoin.getJoinConstraint();
                joinMap.addExportedJoin(mappedFields, fkColumns, null);
                QSchema fkSchema = fkColumns.get(0).getSchema();

                if (fkSchema.hasOnlyForeignKeys()) {
                    JdbcSchema exSchema = (JdbcSchema) fkJoin.getBaseSchema();
                    Collection<String> fkConstraints = exSchema.getForeignKeyConstraints().keySet();
                    for (String fkConstraint : fkConstraints) {
                        QJoin j2 = exSchema.getJoinByForeignKeyConstraints(fkConstraint);
                        if (j2 != fkJoin) {// && linkedSchema != baseSchema) {
                            joinMap.addExportedJoin(mappedFields, fkColumns, j2);
                        }
                    }
                }
            }
            return joinMap;
        }


    }
}
