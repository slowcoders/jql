package org.eipgrid.jql.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;

import java.util.Collection;
import java.util.Map;

public class JqlParser {

    private final ObjectMapper om;

    public JqlParser(ObjectMapper om) {
        this.om = om;
    }

    public JqlFilter parse(QSchema schema, Map<String, Object> filter) {
        JqlFilter where = new JqlFilter(schema);
        if (filter != null) {
            this.parse(where.getPredicateSet(), filter, true);
        }
        return where;
    }

    public void parse(PredicateSet predicates, Map<String, Object> filter, boolean excludeConstantAttribute) {
        // "joinColumn명" : { "id@?EQ" : "joinedColumn2.joinedColumn3.columnName" }; // Fetch 자동 수행.
        //   --> @?EQ 기능은 넣되, 숨겨진 고급기능으로..
        // "groupBy@" : ["attr1", "attr2/attr3" ]

        EntityFilter baseFilter = predicates.getBaseFilter();

        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            int op_start = key.lastIndexOf('@');
            String function = null;
            if (op_start >= 0) {
                function = key.substring(op_start + 1).toLowerCase().trim();
                key = key.substring(0, op_start).trim();
            }

            PredicateFactory op = PredicateFactory.getFactory(function);

            /** [has not 구현]
                SELECT
                    t_0.*, pt_1.episode_id --, t_1.published
                FROM starwars.character as t_0
                    left join starwars.character_episode_link as pt_1 on
                    t_0.id = pt_1.character_id and pt_1.episode_id = 'JEDI'
                where pt_1.character_id is null;
                ---------------------------------
                while (key.startsWith("../")) {
                    baseFilter = baseFilter.getParentNode();
                    key = key.substring(3);
                }
             */

            if (!baseFilter.isJsonNode() && !isValidKey(key)) {
                if (op.isAttributeNameRequired()) {
                    if (op_start == 0) {
                        throw new IllegalArgumentException("Property name is missing : " + entry.getKey());
                    }
                    throw new IllegalArgumentException("invalid JQL key: " + entry.getKey());
                }
                key = null;
            }

            NodeType valueNodeType = this.getNodeType(value);
            EntityFilter subFilter = baseFilter.getFilterNode(key, valueNodeType);
            PredicateSet targetPredicates = predicates;
            if (subFilter != baseFilter) {
                targetPredicates = subFilter.getPredicateSet();
            }

            if (valueNodeType != NodeType.Leaf) {
                PredicateSet ps = op.getPredicates(subFilter, valueNodeType);
                if (valueNodeType == NodeType.Entity) {
                    Map<String, Object> subJql = (Map<String, Object>) value;
                    if (!subJql.isEmpty()) {
                        this.parse(ps, subJql, true);
                    }
                }
                else {  // ValueNodeType.Entities
                    for (Map<String, Object> map : (Collection<Map<String, Object>>) value) {
                        PredicateSet and_qs = new PredicateSet(Conjunction.AND, ps.getBaseFilter());
                        this.parse(and_qs, map, false);
                        ps.add(and_qs);
                    }
                }
                continue;
            }

            String columnName = subFilter.getColumnName(key);
            QColumn column;
            QSchema schema = subFilter.getSchema();
            if (schema != null) {
                column = schema.getColumn(columnName);
                Class<?> fieldType = column.getValueType();
                Class<?> accessType = op.getAccessType(value, fieldType);
                if (value != null) {
                    value = om.convertValue(value, accessType);
//                      value = conversionService.convert(value, accessType);
                }
            }
            else {
                column = new JsonColumn(columnName, value == null ? String.class : value.getClass());
            }
            Expression cond = op.createPredicate(column, value);
            targetPredicates.add(cond);
        }
    }

    private boolean isValidKey(String key) {
        int key_length = key.length();
        if (key_length == 0) return false;
        char ch = key.charAt(0);

        if (!Character.isJavaIdentifierStart(ch) && ch != '+') {
            return false;
        }
        for (int i = key.length(); --i > 0; ) {
            ch = key.charAt(i);
            if (ch != '.' && !Character.isJavaIdentifierPart(ch)) {
                return false;
            }
        }
        return true;
    }

    private NodeType getNodeType(Object value) {
        if (value instanceof Collection) {
            Collection values = (Collection) value;
            if (!values.isEmpty() && values.iterator().next() instanceof Map) {
                return NodeType.Entities;
            }
        }
        if (value instanceof Map) {
            return NodeType.Entity;
        }
        return NodeType.Leaf;
    }


    enum NodeType {
        Leaf,
        Entity,
        Entities
    }
}

