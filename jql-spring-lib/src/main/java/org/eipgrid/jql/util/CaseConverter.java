package org.eipgrid.jql.util;


import java.util.ArrayList;
import java.util.List;

public interface CaseConverter {

    CaseConverter bypassConverter = new BypassConverter();
    CaseConverter camelCaseConverter = new CamelCaseConverter();
    CaseConverter defaultConverter = camelCaseConverter;

    String toPhysicalColumnName(String fieldName);

    String toLogicalAttributeName(String columnName);

    default String[] toPhysicalColumnNames(String[] fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) return null;
        String[] out = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i ++) {
            String key = fieldNames[i];
            out[i] = toPhysicalColumnName(key);
        }
        return out;
    }

    default List<String> toPhysicalColumnNames(Iterable<String> fieldNames) {
        ArrayList<String> out = new ArrayList<>();
        for (String key : fieldNames) {
            out.add(toPhysicalColumnName(key));
        }
        return out;
    }

    static String toSnakeCase(String name) {
        StringBuilder builder = new StringBuilder(name.replace('.', '_'));

        for (int i = 1; i < builder.length(); ++i) {
            if (CamelCaseConverter.isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i))) {
                builder.insert(i++, '_');
            }
        }
        return builder.toString().toLowerCase();
    }

    static String toCamelCase(String str, final boolean capitalizeFirstLetter) {
        if (str == null || str.trim().length() == 0) {
            return str;
        }
        str = str.toLowerCase();
        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        boolean capitalizeNext = false;
        if (capitalizeFirstLetter) {
            capitalizeNext = true;
        }
        for (int index = 0; index < strLen;) {
            final int codePoint = str.codePointAt(index);

            if (codePoint == '_') {
                capitalizeNext = outOffset != 0;
                index += Character.charCount(codePoint);
            } else if (capitalizeNext || outOffset == 0 && capitalizeFirstLetter) {
                final int titleCaseCodePoint = Character.toTitleCase(codePoint);
                newCodePoints[outOffset++] = titleCaseCodePoint;
                index += Character.charCount(titleCaseCodePoint);
                capitalizeNext = false;
            } else {
                newCodePoints[outOffset++] = codePoint;
                index += Character.charCount(codePoint);
            }
        }
        if (outOffset != 0) {
            return new String(newCodePoints, 0, outOffset);
        }
        return str;
    }

    class BypassConverter implements CaseConverter {
        @Override
        public String toPhysicalColumnName(String fieldName) {
            return fieldName;
        }

        @Override
        public String toLogicalAttributeName(String columnName) {
            return columnName;
        }

    };


    class CamelCaseConverter implements CaseConverter {

        public CamelCaseConverter() {
        }

        @Override
        public String toPhysicalColumnName(String fieldName) {
            String name = fieldName;
            if (name.charAt(0) != '_') {
                name = toSnakeCase(fieldName);
            }
            return name;
        }

        @Override
        public String toLogicalAttributeName(String columnName) {
            String name = columnName;
            if (columnName.charAt(0) != '_') {
                name = toCamelCase(columnName, false);
            }
            return name;
        }

        static boolean isUnderscoreRequired(char before, char current) {
            return Character.isLowerCase(before) && Character.isUpperCase(current);
        }
    }

}
