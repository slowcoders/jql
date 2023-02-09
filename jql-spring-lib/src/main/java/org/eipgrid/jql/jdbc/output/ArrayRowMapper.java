package org.eipgrid.jql.jdbc.output;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ArrayRowMapper implements ResultSetExtractor<List<Object[]>> {


    @Override
    public List<Object[]> extractData(ResultSet rs) throws SQLException, DataAccessException {

        int colCount = rs.getMetaData().getColumnCount();
        ArrayList<Object[]> arrayList = new ArrayList<>();

        Object[] pks = new Object[colCount];
        while (rs.next()) {
            for (int i = 0; i < colCount; i++) {
                pks[i] = rs.getObject(i + 1);
            }
            arrayList.add(pks);
        }
        return arrayList;
    }

}
