package org.eipgrid.jql.jdbc.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcIndex {

    String table_schem;
    String table_name;
    String index_qualifier;
    String index_name;
    String column_name;
    String filter_condition;

    boolean is_unique;
    /** +1: Asc, -1: Desc, 0: Unknown; */
    int asc_or_desc;

    int type;
    int ordinal_position;
    int cardinality;
    int pages;

    public JdbcIndex(ResultSet rs) throws SQLException {

        table_schem = rs.getString("table_schem");
        table_name = rs.getString("table_name");
        index_qualifier = rs.getString("index_qualifier");
        index_name = rs.getString("index_name");
        column_name = rs.getString("column_name");
        filter_condition = rs.getString("filter_condition");
        is_unique = !rs.getBoolean("non_unique");

        String sort = rs.getString("asc_or_desc");
        if (sort != null) {
            switch (sort.charAt(0)) {
                case 'A':
                    asc_or_desc = +1;
                    break;
                case 'D':
                    asc_or_desc = -1;
                    break;
                default:
                    throw new RuntimeException("unknown index sort: " + sort);
            }
        }

        type = rs.getInt("type");
        ordinal_position = rs.getInt("ordinal_position");
        cardinality = rs.getInt("cardinality");
        pages = rs.getInt("pages");


        String table_cat = rs.getString("table_cat");
        assert(table_cat == null);
    }
}
