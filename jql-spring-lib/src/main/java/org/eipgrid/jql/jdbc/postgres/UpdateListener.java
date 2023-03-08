package org.eipgrid.jql.jdbc.postgres;

import lombok.SneakyThrows;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.jpa.JpaAdapter;
import org.eipgrid.jql.schema.AutoClearEntityCache;
import org.eipgrid.jql.schema.AutoUpdateModifyTimestamp;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QSchema;
import org.postgresql.jdbc.PgConnection;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class UpdateListener extends Thread {
    private final JdbcStorage storage;
    private final JpaAdapter repository;
    private final PgConnection conn;
    private final ConnectionHolder connHolder;

    public UpdateListener(JdbcStorage storage, String event, JpaAdapter repository) throws SQLException {
        this.storage = storage;
        this.conn = storage.getDataSource().getConnection().unwrap(PgConnection.class);
        this.connHolder = new ConnectionHolder(this.conn);
        this.connHolder.requested();
        this.repository = repository;
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("LISTEN " + event);
        stmt.close();
    }

    public static <ID, ENTITY> void initAutoUpdateTrigger(JdbcStorage storage, JpaAdapter<ENTITY,ID> repository) {
        Class<?> entityType = repository.getEntityType();
        AutoUpdateModifyTimestamp autoUpdate = entityType.getAnnotation(AutoUpdateModifyTimestamp.class);
        AutoClearEntityCache autoClearCache = entityType.getAnnotation(AutoClearEntityCache.class);
        if (autoUpdate == null && autoClearCache == null) return;

        QSchema schema = repository.getSchema();
        String colName = autoUpdate.column();
        String tablePath = schema.getTableName();
        String tableName = tablePath.replace('.', '_');
        List<QColumn> pkColumns = schema.getPKColumns();
        String firstPrimaryKey = pkColumns.get(0).getPhysicalName();
        if (pkColumns.size() > 1) {
            throw new RuntimeException("can not listen update event on table with multi primary keys. ");
        }

        String sql = "CREATE OR REPLACE FUNCTION auto_update__${TABLE}__modify_time()\n" +
                "RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    NEW.${COLUMN} = now();\n";
        if (autoClearCache != null) {
            sql += "   PERFORM pg_notify('${TABLE}_updated', NEW.${PK}::text);\n";
        }
        sql +=  "    RETURN NEW;\n" +
                "END;\n" +
                "$$\n" +
                "LANGUAGE PLPGSQL;\n" +
                "CREATE OR REPLACE TRIGGER ${TABLE}__modify_time_trigger\n" +
                "    BEFORE UPDATE OR INSERT\n" +
                "    ON ${TABLE_PATH}\n" +
                "    FOR EACH ROW EXECUTE PROCEDURE auto_update__${TABLE}__modify_time();";
        sql = sql.replace("${TABLE}", tableName)
                .replace("${TABLE_PATH}", tablePath)
                .replace("${PK}", firstPrimaryKey)
                .replace("${COLUMN}", colName);

        try {
            storage.getJdbcTemplate().execute(sql);
            if (autoClearCache != null) {
                new UpdateListener(storage, tableName + "_updated", repository).start();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        while (true) {
            try {
                // issue a dummy query to contact the backend
                // and receive any pending notifications.
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                rs.close();
                stmt.close();

                org.postgresql.PGNotification notifications[] = conn.getNotifications();
                if (notifications != null) {
                    storage.getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                        @SneakyThrows
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            for (int i = 0; i < notifications.length; i++) {
                                Object id = repository.convertId(notifications[i].getParameter());
                                repository.removeEntityCache(id);
                                System.out.println("Got notification: " + notifications[i].getName() + ": " + id);
                            }
                        }
                    });
                }

                // wait a while before checking again for new
                // notifications
                Thread.sleep(500);
            } catch (SQLException sqle) {
                try {
                    if (conn.isClosed()) break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
                sqle.printStackTrace();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
