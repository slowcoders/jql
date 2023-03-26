package org.eipgrid.jql;

import java.util.*;

public class JqlSelect {

    public static final char LeafProperties = '*';
    public static final char PrimaryKeys = '0';

    public static JqlSelect Auto = new JqlSelect((String) null);

    private final ArrayList<String> propertyNames = new ArrayList<>();

    private ResultMap resultMap = new ResultMap(false);

    private JqlSelect(String selectSpec) {
        parsePropertySelection(selectSpec);
    }

    private JqlSelect(String[] selectSpec) {
        for (String s : selectSpec) {
            parsePropertySelection(s);
        }
    }

    private static String trimToNull(String s) {
        if (s != null) {
            s = s.trim();
            if (s.length() == 0) return null;
        }
        return s;
    }
    public static JqlSelect of(String selectSpec) {
        selectSpec = trimToNull(selectSpec);
        if (selectSpec == null) return Auto;

        JqlSelect select = new JqlSelect(selectSpec);
        return select;
    }
    public static JqlSelect of(String[] selectSpec) {
        if (selectSpec == null || selectSpec.length == 0) return Auto;

        JqlSelect select = new JqlSelect(selectSpec);
        return select;
    }

    public static JqlSelect of(Map<String, Object> selectSpec) {
        if (selectSpec == null || selectSpec.isEmpty()) return Auto;

        JqlSelect select = JqlSelect.of("*");
        select.parsePropertySelection(select.resultMap, "", selectSpec);
        return select;
    }

    private void parsePropertySelection(ResultMap resultMap, String baseKey, Map<String, Object> selectSpec) {
        for (Map.Entry<String, Object> e : selectSpec.entrySet()) {
            String key = e.getKey();
            if (key.indexOf('.') > 0) {
                parsePropertySelection(resultMap, baseKey, 0, key);
            }
            Object value = e.getValue();
            if (value instanceof Map) {
                parsePropertySelection(resultMap.makeSubMap(key), baseKey + key + '.', (Map<String, Object>) value);
            } else {
                resultMap.selectAllLeaf = true;
            }
        }
    }

    private void parsePropertySelection(String selector) {
        selector = trimToNull(selector);
        if (selector == null) return;

        int idx = parsePropertySelection(resultMap, "", 0, selector);
        if (idx < selector.length()) {
            throw new IllegalArgumentException("Syntax error at " + idx + ": [" + selector  + "]");
        }
    }


    private int parsePropertySelection(ResultMap resultMap, String base, int start, String select) {
        String key;
        int idx;
        scan_key: for (idx = start; idx < select.length(); idx ++) {
            int ch = select.charAt(idx);
            switch (ch) {
                case '(': {
                    key = select.substring(start, idx);
                    ResultMap subMap = resultMap.makeSubMap(key);
                    idx = parsePropertySelection(subMap, base + key + '.', idx + 1, select);
                    start = idx + 1;
                    break;
                }
                case ')':
                    break scan_key;

                case ',': {
                    key = select.substring(start, idx).trim();
                    resultMap.addProperty(key);
                    propertyNames.add(base + key);
                    start = idx + 1;
                    break;
                }
            }
        }
        if (start < idx) {
            key = select.substring(start, idx).trim();
            if (key.length() > 0) {
                resultMap.addProperty(key);
                propertyNames.add(base + key);
            }
        }
        return idx;
    }


    public ResultMap getPropertyMap() {
        return resultMap;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public static class ResultMap extends HashMap<String, ResultMap> {
        private boolean selectAllLeaf;
        private boolean selectAllPrimaryKeys;
        private static ResultMap defaultMap = new ResultMap(true);

        public ResultMap(boolean selectAllLeaf) {
            this.selectAllLeaf = selectAllLeaf;
        }

        public boolean isAllLeafSelected() {
            return selectAllLeaf;
        }

        public boolean isIdSelected() {
            return selectAllPrimaryKeys;
        }

        final void addProperty(String key) {
            int p = key.lastIndexOf('.');
            ResultMap base = this;
            if (p > 0) {
                String subKey = key.substring(0, p);
                base = base.makeSubMap(subKey);
                key = key.substring(p + 1).trim();
            }

            if (key.length() == 1) {
                switch (key.charAt(0)) {
                    case LeafProperties:
                        base.selectAllLeaf = true;
                        return;
                    case PrimaryKeys:
                        base.selectAllPrimaryKeys = true;
                        return;
                    default:
                }
            }

            base.put(key, defaultMap);
        }

        final ResultMap makeSubMap(String key) {
            int p = key.indexOf('.');
            ResultMap base = this;
            if (p > 0) {
                String subKey = key.substring(0, p);
                base = base.makeSubMap(subKey);
                key = key.substring(p + 1).trim();
                if (key.length() == 0) {
                    return base;
                }
            }

            ResultMap subMap = base.get(key);
            if (subMap == null || subMap == defaultMap) {
                subMap = new ResultMap(subMap != null);
                base.put(key, subMap);
            }
            return subMap;
        }

    }
}
