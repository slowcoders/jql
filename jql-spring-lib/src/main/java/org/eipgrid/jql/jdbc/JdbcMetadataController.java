package org.eipgrid.jql.jdbc;

import io.swagger.v3.oas.annotations.Operation;
import org.eipgrid.jql.jdbc.storage.JdbcSchema;
import org.eipgrid.jql.js.JsUtil;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.KVEntity;
import org.eipgrid.jql.util.SourceWriter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Map;

public abstract class JdbcMetadataController {

    private final JdbcStorage storage;

    public JdbcMetadataController(JdbcStorage storage) {
        this.storage = (JdbcStorage) storage;
    }


    private QSchema getSchema(String namespace, String tableName) throws Exception {
        String tablePath = namespace + '.' + tableName;
        return storage.loadSchema(tablePath);
    }

    @GetMapping("/{namespace}/{table}")
    @ResponseBody
    @Operation(summary = "table column 목록")
    public Map columns(@PathVariable("namespace") String namespace,
                                @PathVariable("table") String tableName) throws Exception {
        QSchema schema = getSchema(namespace, tableName);
        ArrayList<String> columns = new ArrayList<>();
        for (QColumn column : schema.getPrimitiveColumns()) {
            columns.add(column.getJsonKey());
        }
        ArrayList<String> refs = new ArrayList<>();
        for (QColumn column : schema.getObjectColumns()) {
            refs.add(column.getJsonKey());
        }
        for (Map.Entry<String, QJoin> entry : schema.getEntityJoinMap().entrySet()) {
            if (!entry.getValue().getTargetSchema().hasOnlyForeignKeys()) {
                refs.add(entry.getKey());
            }
        }
        KVEntity entity = KVEntity.of("columns", columns);
        if (refs.size() > 0) {
            entity.put("references", refs);
        }
        return entity;
    }


    @GetMapping("/{namespace}/{table}/{type}")
    @ResponseBody
    @Operation(summary = "Schema 소스 생성")
    public String jsonSchema(@PathVariable("namespace") String namespace,
                             @PathVariable("table") String tableName,
                             @PathVariable("type") SchemaType type) throws Exception {
        if ("*".equals(tableName)) {
            switch (type) {
                case Javascript:
                    return dumpJsonSchemas(namespace);
                case SpringJPA:
                    return dumpJpaSchemas(namespace);
                default:
                    return "";
            }
        }

        QSchema schema = getSchema(namespace, tableName);
        String source;
        if (type == SchemaType.Simple) {
            source = JsUtil.getSimpleSchema(schema);
        }
        else if (type == SchemaType.Javascript) {
            source = JsUtil.createDDL(schema);
            String join = JsUtil.createJoinJQL(schema);
            StringBuilder sb = new StringBuilder();
            sb.append(source).append("\n\n").append(join);
            source = sb.toString();
        }
        else {
            if (schema instanceof JdbcSchema) {
                SourceWriter sb = new SourceWriter('\"');
                ((JdbcSchema)schema).dumpJPAEntitySchema(sb,true);
                source = sb.toString();
            }
            else {
                source = tableName + " is not a JdbcSchema";
            }
        }
        return source;
    }

    private String dumpJpaSchemas(String namespace) throws Exception {
        SourceWriter sb = new SourceWriter('\"');
        JdbcSchema.dumpJPAHeader(sb, true);
        sb.write("public interface " + namespace + " {\n\n");
        sb.incTab();
        for (String tableName : storage.getTableNames(namespace)) {
            QSchema schema = getSchema(namespace, tableName);
            if (schema instanceof JdbcSchema) {
                ((JdbcSchema) schema).dumpJPAEntitySchema(sb,false);
                sb.write("\n\n");
            }
        }
        sb.decTab();
        sb.writeln("}");
        return sb.toString();
    }
    private String dumpJsonSchemas(String namespace) throws Exception {
        SourceWriter sb = new SourceWriter('\'');
        for (String tableName : storage.getTableNames(namespace)) {
            QSchema schema = getSchema(namespace, tableName);
            String source = JsUtil.createDDL(schema);
            String join = JsUtil.createJoinJQL(schema);
            sb.write(source).write("\n\n").write(join);
            sb.write("\n\n");
        }
        return sb.toString();
    }

    @GetMapping("/{namespace}")
    @ResponseBody
    @Operation(summary = "Table 목록")
    public String listTables(@PathVariable("namespace") String namespace) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String tableName : storage.getTableNames(namespace)) {
            sb.append(tableName).append('\n');
        }
        return sb.toString();
    }

    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "Namespace(schema or catalog) 목록")
    public String listNamespaces() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String tableName : storage.getNamespaces()) {
            sb.append(tableName).append('\n');
        }
        return sb.toString();
    }

    enum SchemaType {
        Simple,
        Javascript,
        SpringJPA
    }
}
