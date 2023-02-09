package org.eipgrid.jql;

import java.util.ArrayList;
import java.util.List;

public class JqlSelect {

    private final ArrayList<String> propertyNames = new ArrayList<>();

    public static char All = '*';

    public static char PrimaryKeys = '0';

    public static JqlSelect Auto = new JqlSelect();


    private JqlSelect() {}

    public static JqlSelect of(String selectSpec) {
        JqlSelect select = new JqlSelect();
        select.parsePropertySelection(selectSpec);
        return select;
    }

    public static JqlSelect of(String[] selectSpec) {
        JqlSelect select = new JqlSelect();
        for (String s : selectSpec) {
            select.parsePropertySelection(s);
        }
        return select;
    }

    private void parsePropertySelection(String selector) {
        if (selector == null) return;
        int idx = parsePropertySelection("", 0, selector);
        if (idx < selector.length()) {
            throw new IllegalArgumentException("Syntax error at " + idx + ": [" + selector  + "]");
        }
    }
    private int parsePropertySelection(String base, int start, String select) {
        String key;
        int idx;
        scan_key: for (idx = start; idx < select.length(); idx ++) {
            int ch = select.charAt(idx);
            switch (ch) {
                case '(':
                    key = select.substring(start, idx).trim();
                    if (key.charAt(key.length()-1) != '.') {
                        // key.equals("@to_array")
                        return idx;
                    }
                    idx = parsePropertySelection(base + key, idx + 1, select);
                    start = idx + 1;
                    break;

                case ')':
                    break scan_key;

                case ',':
                    key = select.substring(start, idx).trim();
                    propertyNames.add(base + key);
                    start = idx + 1;
                    break;
            }
        }
        if (start < idx) {
            key = select.substring(start, idx).trim();
            if (key.length() > 0) {
                propertyNames.add(base + key);
            }
        }
        return idx;
    }


    public List<String> getPropertyNames() {
        return propertyNames;
    }
}
