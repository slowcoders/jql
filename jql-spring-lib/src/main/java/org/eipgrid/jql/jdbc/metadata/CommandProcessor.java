package org.eipgrid.jql.jdbc.metadata;

import com.zaxxer.hikari.HikariDataSource;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.js.JsUtil;
import org.eipgrid.jql.util.CaseConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommandProcessor implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

        String jdbcUrl = args[0]; // "jdbc:postgresql://0.0.0.127/postgres";
        String jdbcDriver = "org.postgresql.Driver";
        String userName = args[1];
        String password = args[2];

        DataSource ds = DataSourceBuilder.create()
                .driverClassName(jdbcDriver)
                .type(HikariDataSource.class)
                .url(jdbcUrl)
                .username(userName)
                .password(password)
                .build();

//        Connection conn = ds.getConnection();

        JdbcSchemaLoader mp = new JdbcSchemaLoader(null, ds, CaseConverter.defaultConverter);
        for (String dbSchema : mp.getDBSchemas()) {
            List<String> tableNames = mp.getTableNames(dbSchema);
            ArrayList<QSchema> schemas = new ArrayList<>();
            for (String tableName : tableNames) {
                schemas.add(mp.loadSchema(dbSchema + '.' + tableName));
            }

            for (QSchema schema : schemas) {
                ((JdbcSchema)schema).dumpJPAEntitySchema();
            }
            for (QSchema schema : schemas) {
                String ddl = "";//schema.generateDDL();
//                System.out.println(ddl);
            }
            for (QSchema schema : schemas) {
                String ddl = JsUtil.createJoinJQL(schema);
                System.out.println(ddl);
            }
        }

        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        new CommandProcessor().run(args);
    }

}