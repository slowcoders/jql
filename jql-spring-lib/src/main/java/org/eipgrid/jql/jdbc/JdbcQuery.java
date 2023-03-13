package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.JqlSelect;
import org.eipgrid.jql.OutputFormat;
import org.eipgrid.jql.parser.JqlFilter;
import org.springframework.data.domain.Sort;

import java.util.List;

public class JdbcQuery<ENTITY> extends JqlQuery<ENTITY> {

    //protected static int SingleEntityOffset = JqlQuery.SingleEntityOffset;
    private final JdbcRepositoryBase table;
    private final JqlFilter filter;
    /*package*/ String executedQuery;
    /*package*/ Object extraInfo;

    public JdbcQuery(JdbcRepositoryBase table, JqlSelect select, JqlFilter filter) {
        assert (filter != null);
        this.table = table;
        this.filter = filter;
        super.select(select);
    }

    public final Class<ENTITY> getJpaEntityType() {
        return filter.getJpqlEntityType();
    }

    @Override
    public JqlSelect getSelection() {
        JqlSelect select = super.getSelection();
        if (select == null || select == JqlSelect.Auto) {
            select = new JqlSelect();//.of("");
            super.select(select);
        }
        return select;
    }

    @Override
    protected void select(JqlSelect select) {
        super.select(select);
        invalidateCache(false);
    }

    @Override
    public JdbcQuery<ENTITY> sort(Sort sort) {
        super.sort(sort);
        invalidateCache(false);
        return this;
    }

    private void invalidateCache(boolean all) {
        executedQuery = null;
        if (all) extraInfo = null;
    }

    @Override
    public List<ENTITY> getResultList(OutputFormat outputType) {
        return table.find(this, outputType);
    }

    @Override
    public long count() {
        return table.count(this);
    }

    public JqlFilter getFilter() {
        return this.filter;
    }

    public String getExecutedQuery() {
        return executedQuery;
    }

    public Object getExtraInfo() {
        return extraInfo;
    }

    public String appendPaginationQuery(String sql) {
        String s = "";
        if (getOffset() > 0) {
            s += "\nOFFSET " + getOffset();
        }
        if (getLimit() > 0) {
            s += "\nLIMIT " + getLimit();
        }
        if (s.length() > 0) {
            sql += s;
        }
        return sql;
    }
}
