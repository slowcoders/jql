package org.eipgrid.jql.parser;

public enum Conjunction {
    AND(" and "),
    OR(" or ");

    private final String text;

    Conjunction(String delimiter) {
        this.text = delimiter;
    }

    @Override
    public String toString() {
        return text;
    }
}
