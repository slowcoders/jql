package org.eipgrid.jql.parser;

import org.eipgrid.jql.schema.QColumn;

import java.util.Collection;

public interface PredicateVisitor {

    void visitPredicate(QColumn column, JqlOp operator, Object value);

    void visitNot(Expression statement);

    void visitMatchAny(QColumn column, JqlOp operator, Collection values);

    void visitCompareNull(QColumn column, JqlOp operator);

    void visitPredicates(Collection<Expression> predicates, Conjunction conjunction);

    void visitAlwaysTrue();
}
