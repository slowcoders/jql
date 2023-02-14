package org.eipgrid.jql.csv;

import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.util.ClassUtils;
import org.eipgrid.jql.util.KVEntity;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParser {
    private final List<CsvColumn> schema;
    private static HashMap<Class, List<CsvColumn>> caches = new HashMap<>();

    public CsvParser(Class entityType) {
        this(getSchema(entityType));
    }

    private CsvParser(List<CsvColumn> schema) {
        this.schema = schema;
    }

    private static List<CsvColumn> getSchema(Class entityType) {
        List<CsvColumn> schema = caches.get(entityType);
        if (schema == null) {
            schema = new ArrayList<>();
            List<Field> fields = ClassUtils.getFields(entityType, 0);
            for (Field f : fields) {
                schema.add(new CsvColumn(f));
            }
            caches.put(entityType, schema);
        }
        return schema;
    }

    public void parseCSV(Reader reader, Map<String, Object> entity) {
        Iterable<CsvColumn> columns = schema;
        StringBuilder sb = new StringBuilder();
        for (CsvColumn column : columns) {
            try {
                Object value;
                JsType format = column.getValueType();
                if (format == JsType.Object) {
                    value = readCsv(reader, column.getElementType());
                }
                else {
                    String text = readRaw(reader, sb);
                    if (text == null && column.isNullable()) {
                        continue;
                    }

                    switch (format) {
                        case Boolean:
                            value = Boolean.parseBoolean(text);
                            break;
                        case Integer:
                            value = Long.parseLong(text);
                            break;
                        case Float:
                            value = Double.parseDouble(text);
                            break;
                        case Text:
                            value = text;
                            break;
                        case Time:
                        case Date:
                        case Timestamp:
                            value = text;
                            break;
                        case Array:
                            if (text != null) {
                                throw new IllegalStateException("Collection must be delimited by empty column. but we got '" + text + "'");
                            }
                            value = readCsvCollection(reader, column.getElementType());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + format);
                    }
                }
                entity.put(column.getName(), value);
            }
            catch (Exception e) {
                throw new RuntimeException("Wrong data for " + column.getName(), e);
            }
        }
    }

    private Object readCsv(Reader reader, Class<?> javaType) throws IOException {
        List<CsvColumn> subSchema = getSchema(javaType);
        CsvParser parser = new CsvParser(subSchema);
        KVEntity entity = new KVEntity();
        parser.parseCSV(reader, entity);
        return entity;
    }

    private ArrayList readCsvCollection(Reader reader, Class<?> javaType) throws IOException {
        List<CsvColumn> subSchema = getSchema(javaType);
        ArrayList<Object> entities = new ArrayList<>();
        CsvParser parser = new CsvParser(subSchema);
        read_collection: while (reader.ready()) {
            KVEntity entity = new KVEntity();
            parser.parseCSV(reader, entity);
            entities.add(entity);
            int last_ch = reader.read();
            if (last_ch == 0x0D) {
                last_ch = reader.read();
            }
            switch (last_ch) {
                case '\n':
                case -1:
                    break read_collection;
                case ',':
                    break;
                default:
                    throw new RuntimeException("something wrong with schema");
            }
        }
        return entities;
    }

    private String readRaw(Reader reader, StringBuilder sb) throws IOException {
        sb.setLength(0);
        int delimiter = ',';
        boolean isFirst = true;
        while (true) {
            int ch = reader.read();
            if (ch == '\\') {
                ch = reader.read();
            }
            else if (ch == delimiter || ch < 0) {
                break;
            }
            else if (isFirst) {
                isFirst = false;
                if (ch == '\'' || ch == '"') {
                    delimiter = ch;
                }
            }
            sb.append((char)ch);
        }
        if (sb.length() == 0) {
            return null;
        }
        String s = sb.toString().trim();
        return s;
    }

}
