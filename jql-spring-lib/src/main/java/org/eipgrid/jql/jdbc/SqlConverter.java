package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.parser.*;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.util.SourceWriter;

import java.util.*;

public abstract class SqlConverter implements PredicateVisitor {
    protected final SourceWriter sw;
    public enum Command {
        Insert,
        Delete,
        Update,
    }

    public SqlConverter(SourceWriter sw) {
        this.sw = sw;
    }


    protected abstract void writeQualifiedColumnName(QColumn column, Object value);

    @Override
    public void visitPredicate(QColumn column, JqlOp operator, Object value) {
        writeQualifiedColumnName(column, value);
        String op = "";
        assert (value != null);
        switch (operator) {
            case EQ:
                op = " = ";
                break;
            case NE:
                op = " != ";
                break;

            case GE:
                op = " >= ";
                break;
            case GT:
                op = " > ";
                break;

            case LE:
                op = " <= ";
                break;
            case LT:
                op = " < ";
                break;

            case LIKE:
                op = " LIKE ";
                break;
            case NOT_LIKE:
                op = " NOT LIKE ";
                break;
        }
        sw.write(op).writeValue(value);
    }

    @Override
    public void visitNot(Expression statement) {
        sw.write(" NOT ");
        statement.accept(this);
    }

    @Override
    public void visitMatchAny(QColumn column, JqlOp operator, Collection values) {
        if (operator == JqlOp.EQ || operator == JqlOp.NE) {
            writeQualifiedColumnName(column, values);
        }
        switch (operator) {
            case NE:
                sw.write(" NOT");
                // no-break;
            case EQ:
                sw.write(" IN(");
                sw.writeValues(values);
                sw.write(")");
                break;

            case NOT_LIKE:
                sw.write(" NOT ");
                // no-break;
            case LIKE:
                sw.write("(");
                boolean first = true;
                for (Object v : values) {
                    if (first) {
                        first = false;
                    } else {
                        sw.write(" OR ");
                    }
                    writeQualifiedColumnName(column, "");
                    sw.write(" LIKE ");
                    sw.writeQuoted(v);
                }
                sw.write(")");
                break;

            default:
                throw new RuntimeException("Invalid match any operator: " + operator);
        }
    }

    @Override
    public void visitCompareNull(QColumn column, JqlOp operator) {
        String value;
        switch (operator) {
            case EQ:
                value = " IS NULL";
                break;
            case NE:
                value = " IS NOT NULL";
                break;
            default:
                throw new RuntimeException("Invalid match operator with null value: " + operator);
        }
        writeQualifiedColumnName(column, null);
        //key.printSQL(this);
        sw.write(value);
    }

    @Override
    public void visitAlwaysTrue() {
        sw.write("true");
    }

    @Override
    public void visitPredicates(Collection<Expression> predicates, Conjunction conjunction) {
        sw.write("(");
        boolean first = true;
        for (Expression item : predicates) {
            if (item.isEmpty()) continue;

            if (first) {
                first = false;
            } else {
                sw.write(conjunction.toString());
            }
            item.accept(this);
        }
        sw.write(")");
    }

}
