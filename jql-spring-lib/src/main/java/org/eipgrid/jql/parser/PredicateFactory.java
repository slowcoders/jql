package org.eipgrid.jql.parser;

import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.util.ClassUtils;

import java.util.Collection;
import java.util.HashMap;

abstract class PredicateFactory {


    static PredicateFactory IS = new MatchAny(JqlOp.EQ);

    public Class<?> getAccessType(Object value, Class<?> fieldType) {
        return fieldType;
    }

    public boolean isAttributeNameRequired() { return true; }

    public abstract Predicate createPredicate(QColumn column, Object value);

    public static PredicateFactory getFactory(String function) {
        if (function == null) return IS;
        return operators.get(function);
    }

    public PredicateSet getPredicates(EntityFilter node, JqlParser.NodeType nodeType) {
        PredicateSet basePredicates = node.getPredicateSet();
        switch (nodeType) {
            case Entities: {
                PredicateSet or_qs = new PredicateSet(Conjunction.OR, basePredicates.getBaseFilter());
                basePredicates.add(or_qs);
                return or_qs;
            }
            case Entity: default:
                return basePredicates;
        }
    }

    private static final HashMap<String, PredicateFactory> operators = new HashMap<>();

    //=======================================================//
    // Operators
    // ------------------------------------------------------//

    private static class Compare extends PredicateFactory {
        final JqlOp operator;

        Compare(JqlOp operator) {
            super();
            this.operator = operator;
        }

        public Predicate createPredicate(QColumn column, Object value) {
            return new Predicate.Compare(column, operator, value);
        }
    }

    private static class MatchAny extends PredicateFactory {
        final JqlOp operator;

        MatchAny(JqlOp operator) {
            super();
            this.operator = operator;
        }

        public Class<?> getAccessType(Object value, Class<?> fieldType) {
            if (value.getClass().isArray() || value instanceof Collection) {
                fieldType = ClassUtils.getArrayType(fieldType);
            }
            return fieldType;
        }


        public Predicate createPredicate(QColumn column, Object value) {
            Predicate cond;
            Collection values = value == null ? null : ClassUtils.asCollection(value);
            if (values != null) {
                cond = new Predicate.MatchAny(column, operator, values);
            }
            else {
                cond = new Predicate.Compare(column, operator, value);
            }
            return cond;
        }
    };

    static class NotMatch extends MatchAny {

        NotMatch(JqlOp operator) {
            super(operator);
        }

        public boolean isAttributeNameRequired() { return false; }

        public PredicateSet getPredicates(EntityFilter node, JqlParser.NodeType nodeType) {
            PredicateSet baseScope = node.getPredicateSet();
            switch (nodeType) {
                case Entities: {
                    PredicateSet or_qs = new PredicateSet(Conjunction.OR, baseScope.getBaseFilter());
                    baseScope.add(new Predicate.Not(or_qs));
                    return or_qs;
                }
                case Entity:
                    PredicateSet and_qs = new PredicateSet(Conjunction.AND, baseScope.getBaseFilter());
                    baseScope.add(new Predicate.Not(and_qs));
                    return and_qs;
                default:
                    return baseScope;
            }
        }
    }

    static class PairedPredicate extends PredicateFactory {
        private final PredicateFactory operator1;
        private final PredicateFactory operator2;
        private final Conjunction conjunction;

        PairedPredicate(PredicateFactory operator1, PredicateFactory operator2, Conjunction conjunction) {
            super();
            this.operator1 = operator1;
            this.operator2 = operator2;
            this.conjunction = conjunction;
        }

        public Class<?> getAccessType(Object value, Class<?> fieldType) {
            return ClassUtils.getArrayType(fieldType);
        }

        public Predicate createPredicate(QColumn column, Object value) {
            Object[] range = (Object[])value;
            PredicateSet predicates = new PredicateSet(conjunction);
            predicates.add(operator1.createPredicate(column, range[0]));
            predicates.add(operator2.createPredicate(column, range[1]));
            return predicates;
        }
    }

    static {
        operators.put("is", new MatchAny(JqlOp.EQ));
        operators.put("not", new NotMatch(JqlOp.NE));

        operators.put("like", new MatchAny(JqlOp.LIKE));
        operators.put("not like", new NotMatch(JqlOp.NOT_LIKE));

        Compare GT = new Compare(JqlOp.GT);
        Compare LT = new Compare(JqlOp.LT);
        Compare GE = new Compare(JqlOp.GE);
        Compare LE = new Compare(JqlOp.LE);

        operators.put("gt", GT);
        operators.put("lt", LT);
        operators.put("ge", GE);
        operators.put("le", LE);
        operators.put("between", new PairedPredicate(GE, LE, Conjunction.AND));
        operators.put("not between", new PairedPredicate(LT, GT, Conjunction.OR));
    }
}
