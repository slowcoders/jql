package org.eipgrid.jql.jdbc.timescale;

import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.util.SourceWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class TSDBHelper {

    private static final String SUFFIX_CONT_AGG = "_ts_agg";
    private static final String SUFFIX_HOURLY_VIEW = "_hourly";
    private static final String SUFFIX_DAILY_VIEW = "_daily";
    private static final boolean USE_CONN = true;

    private final String tableName;
    private final JdbcStorage storage;
    private final JdbcTemplate jdbc;

    private QColumn timeKeyColumn;
    private HashMap<String, AggregateType> aggTypeMap;
    private QSchema schema;
    private Connection conn;

    public TSDBHelper(JdbcStorage storage, String tableName) {
        this.jdbc = storage.getJdbcTemplate();
        this.storage = storage;
        this.tableName = tableName;
    }

    private QColumn resolveTimeKeyColumn() {
        for (QColumn column : this.schema.getPKColumns()) {
            if (JsType.of(column.getValueType()) == JsType.Timestamp) {
                return column;
            }
        }
        throw new RuntimeException("Column for the timeKey is not found");
    }

    public AggregateType getAggregationType(QColumn col) {
        AggregateType t = aggTypeMap.get(col.getPhysicalName());
        if (t == null) t = AggregateType.None;
        return t;
    }


    public String getTimeKeyColumnName() {
        return timeKeyColumn.getPhysicalName();
    }

    private boolean isTableExists(String tableName) {
        try {
            if (true) {
                String sql = "SELECT to_regclass('" + tableName + "');";
                String res = jdbc.queryForObject(sql, String.class);
                return res != null;
            }
            else {
                String sql = "SELECT 1 FROM " + tableName + " limit 1";
                queryForList(sql);
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private void queryForList(String sql) throws SQLException {
        if (USE_CONN) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
        }
        else {
            jdbc.queryForList(sql);
        }
    }

    private void execute(String sql) throws SQLException {
        if (USE_CONN) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
        }
        else {
            jdbc.execute(sql);
        }
    }

    private void execute_silently(String sql) {
        try {
            if (USE_CONN) {
                Statement stmt = conn.createStatement();
                stmt.execute(sql);
                stmt.close();
            }
            else {
                jdbc.execute(sql);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    protected void initializeTSDB(QSchema schema) throws SQLException {
        if (conn != null) {
            initializeTSDB_internal(schema);
        }
        else {
            jdbc.execute(new ConnectionCallback<Void>() {
                @Override
                public Void doInConnection(Connection con) throws SQLException, DataAccessException {
                    /**
                     * transaction 내부에서 DB 생성이 불가능한 문제 처리를 위해 별도 Connection 을 생성.
                     */
                    conn = con;
                    boolean autoCommitOld = con.getAutoCommit();
                    try {
                        con.setAutoCommit(true);
                        initializeTSDB_internal(schema);
                    } finally {
                        con.setAutoCommit(autoCommitOld);
                    }
                    return null;
                }
            });
        }
    }

    protected void initializeTSDB_internal(QSchema schema) throws SQLException {
        if (isTableExists(this.tableName + SUFFIX_DAILY_VIEW)) return;

        this.schema = schema;
        this.timeKeyColumn = resolveTimeKeyColumn();
        String sql = build_init_timescale(2);
        execute(sql);

        this.aggTypeMap = resolveAggregationTypeMap();
        remove_down_sampling_view();
        // 6주를 기본 저장 간격으로 설정한다.
        sql = build_auto_down_sampling_view(7 * 6);
        execute(sql);
    }


    protected abstract HashMap<String, AggregateType> resolveAggregationTypeMap();

    private void remove_down_sampling_view() throws SQLException {
        String tableName = schema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;
        execute_silently("SELECT remove_continuous_aggregate_policy('" + aggView + "');");
        execute("DROP MATERIALIZED VIEW IF EXISTS " + aggView + " cascade");
    }

    private void refresh_aggregation(Timestamp start, Timestamp end) throws SQLException {
        String tableName = schema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;

        String sql = "CALL refresh_continuous_aggregate('" + aggView + "', NULL, DATE_TRUNC('hour', now()));";
        execute(sql);
    }

    protected String build_init_timescale(int hours) {
        SourceWriter sb = new SourceWriter('\'');
        String ts_column = getTimeKeyColumnName();
        sb.writeF("SELECT create_hypertable('{0}', '{1}',", schema.getTableName(), ts_column)
                .write("if_not_exists => TRUE, migrate_data => true, ")
                .writeF("chunk_time_interval => interval '{0} hour')", Integer.toString(hours));
        return sb.toString();
    }

    protected String build_auto_down_sampling_view(int retention_days) {
        SourceWriter sb = new SourceWriter('\'');
        String tableName = schema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;
        QColumn ts_key = this.timeKeyColumn;
        String ts_col_name = this.getTimeKeyColumnName();

        sb.write("CREATE MATERIALIZED VIEW IF NOT EXISTS ").writeln(aggView);
        sb.writeln("\tWITH (timescaledb.continuous)\nAS SELECT").incTab();
        sb.writeF("time_bucket('1 hour', {0}) AS time_h,\n", ts_col_name);

        for (QColumn col : schema.getPKColumns()) {
            if (col != ts_key) {
                sb.write(col.getPhysicalName()).writeln(",");
            }
        }

        ArrayList<QColumn> accColumns = new ArrayList<>();
        for (QColumn col : schema.getWritableColumns()) {
            String col_name = col.getPhysicalName();
            switch (getAggregationType(col)) {
                case Sum: {
                    String ss = "min({0}) as {0}_min,\n" +
                            "max({0}) as {0}_max,\n" +
                            "first({0}, {1}) as {0}_first,\n" +
                            "last({0}, {1}) as {0}_last,\n";
                    sb.writeF(ss, col_name, ts_col_name);
                    accColumns.add(col);
                    break;
                }
                case Mean: {
                    String ss = "avg({0}) as {0},\n";
                    sb.writeF(ss, col_name);
                    break;
                }
                case None:
                    break;
            }
        }

        sb.decTab().replaceTrailingComma("\nFROM ").write(schema.getTableName()).writeln();
        sb.write("GROUP BY ");
        for (QColumn col : schema.getPKColumns()) {
            if (col == ts_key) {
                sb.write("time_h, ");
            }
            else {
                sb.write(col.getPhysicalName()).write(", ");
            }
        }
        sb.replaceTrailingComma(";\n\n");

        String retention_interval = retention_days <= 0 ? "NULL" : "INTERVAL '" + retention_days + " day'";
        sb.writeF("SELECT add_continuous_aggregate_policy('{0}',\n", aggView).incTab();
        sb.writeF("start_offset => {0},\n", retention_interval);
        sb.write("end_offset => INTERVAL '1 hour',\n" +
                 "schedule_interval => INTERVAL '1 hour');\n\n");
        sb.decTab();

        if (false && retention_days > 0) {
            // 설치 버전 문제인지 아래 함수 호출에 실패.
            sb.writeF("SELECT add_drop_chunks_policy('{0}', {1}, cascade_to_materializations=>FALSE);\n\n",
                    aggView, retention_interval);
        }

        sb.writeF("CREATE OR REPLACE VIEW {0} AS\nSELECT\n", tableName + SUFFIX_HOURLY_VIEW);
        if (accColumns.size() == 0) {
            sb.writeln("* FROM ").writeln(aggView).write(";\n\n");
        }
        else {
            sb.incTab();
            for (QColumn col : schema.getPKColumns()) {
                if (col == ts_key) {
                    sb.write("time_h, ");
                }
                else {
                    sb.write(col.getPhysicalName()).writeln(",");
                }
            }
            for (QColumn col : schema.getWritableColumns()) {
                switch (getAggregationType(col)) {
                    case Sum: {
                        String fmt =
                                "(case when {0}_last >= least({0}_first, lag({0}_last) over _w) then\n" +
                                "           {0}_max - least({0}_min, lag({0}_last) over _w)\n" +
                                "      else {0}_max - least({0}_first, lag({0}_last) over _w) + ({0}_last - {0}_min)\n" +
                                " end) as {0},\n";
                        sb.writeF(fmt, col.getPhysicalName());
                        break;
                    }
                    case Mean: {
                        sb.write(col.getPhysicalName()).writeln(",");
                        break;
                    }
                }
            }
            sb.decTab();
            sb.replaceTrailingComma("\nFROM ").writeln(aggView);
            sb.write("WINDOW _w AS(partition by ");
            for (QColumn col : schema.getPKColumns()) {
                if (col != ts_key) sb.write(col.getPhysicalName()).write(", ");
            }
            sb.replaceTrailingComma(" ORDER BY time_h);");
            sb.writeln();
        }

        sb.writeln();
        sb.writeF("CREATE OR REPLACE VIEW {0} AS\nSELECT\n", tableName + SUFFIX_DAILY_VIEW).incTab();
        sb.writeln("time_bucket('1 day', time_h) AS time_d,");
        for (QColumn col : schema.getPKColumns()) {
            if (col != ts_key) sb.write(col.getPhysicalName()).writeln(",");
        }
        for (QColumn col : schema.getWritableColumns()) {
            switch (getAggregationType(col)) {
                case Sum: {
                    sb.writeF("sum({0}) as {0},\n", col.getPhysicalName());
                    break;
                }
                case Mean: {
                    sb.writeF("avg({0}) as {0},\n", col.getPhysicalName());
                    break;
                }
            }
        }
        sb.decTab().replaceTrailingComma("\n");
        sb.writeF("FROM {0}\nGROUP BY ", tableName + SUFFIX_HOURLY_VIEW);
        for (QColumn col : schema.getPKColumns()) {
            if (col != ts_key) sb.write(col.getPhysicalName()).write(", ");
        }
        sb.writeln("time_d;");
        return sb.toString();
    }

    public JqlRepository getRepository() throws SQLException {
        Connection con = storage.getDataSource().getConnection();
//        return jdbc.execute(new ConnectionCallback<JQRepository>() {
//            @Override
//            public JQRepository doInConnection(Connection con) throws SQLException, DataAccessException {
                conn = con;

                boolean autoCommitOld = con.getAutoCommit();
                try {
                    con.setAutoCommit(true);
                    if (!isTableExists(tableName)) {
                        String sql = generateDDL(tableName);
                        execute(sql);
                    }

                    QSchema schema = storage.loadSchema(tableName);
                    initializeTSDB(schema);
                    return storage.getRepository(tableName);
                }
                finally {
                    con.setAutoCommit(autoCommitOld);
                    con.close();
                }
//            }
//        });
    }

    protected abstract String generateDDL(String tableName);
}