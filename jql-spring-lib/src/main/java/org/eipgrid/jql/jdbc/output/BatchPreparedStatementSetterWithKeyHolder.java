package org.eipgrid.jql.jdbc.output;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface BatchPreparedStatementSetterWithKeyHolder extends BatchPreparedStatementSetter {

    void setGeneratedKeys(List<Map<String, Object>> keys);

    static int[] batchUpdateWithKeyHolder(JdbcTemplate jdbcTemplate, final String sql, final BatchPreparedStatementSetterWithKeyHolder pss) {
        return jdbcTemplate.execute(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                return con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }
        }, new PreparedStatementCallback<int[]>() {
            @Override
            public int[] doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                try {
                    int batchSize = pss.getBatchSize();
                    InterruptibleBatchPreparedStatementSetter ipss =
                            (pss instanceof InterruptibleBatchPreparedStatementSetter ?
                                    (InterruptibleBatchPreparedStatementSetter) pss : null);
                    int[] result;
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    List<Map<String, Object>> keys = keyHolder.getKeyList();

                    try {
                        if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
                            for (int i = 0; i < batchSize; i++) {
                                pss.setValues(ps, i);
                                if (ipss != null && ipss.isBatchExhausted(i)) break;
                                ps.addBatch();
                            }
                            result = ps.executeBatch();
                            keys = getGeneratedKeys(ps);
                        } else {
                            int rowsAffected[] = new int[batchSize];
                            for (int i = 0; i < batchSize; i++) {
                                pss.setValues(ps, i);
                                if (ipss != null && ipss.isBatchExhausted(i)) {
                                    batchSize = i;
                                    break;
                                }

                                rowsAffected[i] = ps.executeUpdate();
                                keys = getGeneratedKeys(ps);
                            }
                            if (rowsAffected.length > batchSize) {
                                result = new int[batchSize];
                                System.arraycopy(rowsAffected, 0, result, 0, batchSize);
                            } else {
                                result = rowsAffected;
                            }
                        }
                    } finally {
                        pss.setGeneratedKeys(keys);
                    }

                    return result;
                } finally {
                    if (pss instanceof ParameterDisposer) ((ParameterDisposer) pss).cleanupParameters();
                }
            }
        });
    }

    static RowMapperResultSetExtractor keyExtractor =
            new RowMapperResultSetExtractor<Map<String, Object>>(new ColumnMapRowMapper(), 1);
    static List<Map<String, Object>> emptyKeys = new ArrayList<>();

    static List<Map<String, Object>> getGeneratedKeys(PreparedStatement ps) throws SQLException {
        List<Map<String, Object>> keys = emptyKeys;
        ResultSet rs = ps.getGeneratedKeys();
        if (rs != null) {
            try {
                keys = keyExtractor.extractData(rs);
            } finally {
                rs.close();
            }
        }
        return keys;
    }

}