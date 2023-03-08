package org.eipgrid.jql;

import java.util.*;

public class JqlSelect {

    public static char All = '*';
    public static char PrimaryKeys = '0';

    public static JqlSelect Auto = new JqlSelect((String) null);

    private final ArrayList<String> propertyNames = new ArrayList<>();

    private final HashMap<String, Object> selectionMap = new HashMap<>();
    // QResultMapping resultMapping;

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

    private void parsePropertySelection(String selector) {
        selector = trimToNull(selector);
        if (selector == null) return;

        int idx = parsePropertySelection(selectionMap, "", 0, selector);
        if (idx < selector.length()) {
            throw new IllegalArgumentException("Syntax error at " + idx + ": [" + selector  + "]");
        }
    }

    private HashMap<String, Object> makeSubMap(HashMap<String, Object> base, String key) {
        int p = key.indexOf('.');
        if (p > 0) {
            String subKey = key.substring(0, p);
            base = makeSubMap(base, subKey);
            key = key.substring(p + 1).trim();
            if (key.length() == 0) {
                return base;
            }
        }
        HashMap<String, Object> subMap = (HashMap<String, Object>) base.get(key);
        if (subMap == null) {
            subMap = new HashMap<>();
            base.put(key, subMap);
        }
        return subMap;
    }

    private int parsePropertySelection(HashMap<String, Object> map, String base, int start, String select) {
        String key;
        int idx;
        scan_key: for (idx = start; idx < select.length(); idx ++) {
            int ch = select.charAt(idx);
            switch (ch) {
                case '(':
                    key = select.substring(start, idx);
                    HashMap<String, Object> subMap = makeSubMap(map, key);
                    idx = parsePropertySelection(subMap, base + key + '.', idx + 1, select);
                    start = idx + 1;
                    break;

                case ')':
                    break scan_key;

                case ',':
                    key = select.substring(start, idx).trim();
                    makeSubMap(map, key);
                    propertyNames.add(base + key);
                    start = idx + 1;
                    break;
            }
        }
        if (start < idx) {
            key = select.substring(start, idx).trim();
            if (key.length() > 0) {
                makeSubMap(map, key);
                propertyNames.add(base + key);
            }
        }
        return idx;
    }


    public Map<String, Object> getResultMappings() {
        return selectionMap;
    }

    public List<String> getPropertyNames() {
        return propertyNames;
    }

}
