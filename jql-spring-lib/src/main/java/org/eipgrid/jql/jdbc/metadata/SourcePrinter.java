package org.eipgrid.jql.jdbc.metadata;

import java.io.OutputStream;
import java.io.PrintStream;

public class SourcePrinter extends PrintStream {
    private int indent;
    private boolean atStartOfLine;

    SourcePrinter(OutputStream out) {
        super(out);
    }

    public void print(String s) {
        if (s.trim().endsWith("{")) {
            this.indent++;
        }
        super.print(s);
    }

    public void println(String s) {
        if (s.trim().endsWith("{")) {
            this.indent++;
        }
        this.print(s);
        this.atStartOfLine = true;
    }

    public void println() {
        super.println();
        this.atStartOfLine = true;
    }
}