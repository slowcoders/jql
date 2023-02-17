package org.eipgrid.jql.jdbc.output;

import org.eipgrid.jql.jdbc.JdbcTable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IdListMapper<ID> implements ResultSetExtractor<List<ID>> {
    private final JdbcTable<?,ID> repository;

    public IdListMapper(JdbcTable<?, ID> repository) {
        this.repository = repository;
    }


    @Override
    public List<ID> extractData(ResultSet rs) throws SQLException, DataAccessException {

        int pkCount = rs.getMetaData().getColumnCount();
        ArrayList<ID> idList = new ArrayList<>();

        if (pkCount == 1) {
            while (rs.next()) {
                Object pk = rs.getObject(1);
                ID id = repository.convertId(pk);
                idList.add(id);
            }
        }
        else {
            Object[] pks = new Object[pkCount];
            while (rs.next()) {
                for (int i = 0; i <= pkCount; i++) {
                    pks[i] = rs.getObject(i + 1);
                }
                ID id = repository.convertId(pks);
                idList.add(id);
            }
        }
        return idList;
    }

}
