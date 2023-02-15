package org.eipgrid.jql.jdbc.metadata;

import javax.persistence.Table;

class TablePath {
    String catalog;
    String schema;
    String qualifiedName;
    String simpleName;

    TablePath(String catalog, String schema, String qualifiedName, String simpleName) {
        this.catalog = catalog;
        this.schema = schema;
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }


    public static TablePath of(Class<?> clazz, JdbcSchemaLoader schemaLoader) {
        Table table = clazz.getAnnotation(Table.class);
        return table != null ? of(table, schemaLoader) : null;
    }

    public static TablePath of(Table table, JdbcSchemaLoader schemaLoader) {
        String name = table.name().trim();
        String schema = table.schema().trim();
        String catalog = table.catalog().trim();
        return of(catalog, schema, name, schemaLoader);
    }

    public static TablePath of(String qualifiedName, JdbcSchemaLoader schemaLoader) {
        qualifiedName = qualifiedName.toLowerCase();
        int last_dot_p = qualifiedName.lastIndexOf('.');
        String namespace = last_dot_p > 0 ? qualifiedName.substring(0, last_dot_p) : null;//getDefaultDBSchema();
        String simpleName = qualifiedName.substring(last_dot_p + 1);
        return new TablePath(namespace, namespace, qualifiedName, simpleName);
    }

    protected static TablePath of(String db_catalog, String db_schema, String simple_name, JdbcSchemaLoader schemaLoader) {
        String namespace = schemaLoader.isDBSchemaSupported() ? db_schema : db_catalog;
        simple_name = simple_name.toLowerCase();

        if (namespace == null || namespace.length() == 0) {
            namespace = schemaLoader.getDefaultNamespace();
        }
        namespace = namespace.toLowerCase();
        String qname = namespace + "." + simple_name;
        return new TablePath(db_catalog, db_schema, qname, simple_name);
    }


}
