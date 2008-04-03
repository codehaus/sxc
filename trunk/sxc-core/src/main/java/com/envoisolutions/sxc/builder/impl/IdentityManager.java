package com.envoisolutions.sxc.builder.impl;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Arrays;

public class IdentityManager {
    private final Set<String> ids = new TreeSet<String>();

    public boolean containsId(String id) {
        return ids.contains(id);
    }

    public void addId(String id) {
        if (ids.contains(id)) {
            throw new IllegalArgumentException("Id already defined " + id);
        }
        ids.add(id);
    }

    public String createId(String id) {
        id = toValidId(id);

        if (!ids.contains(id)) {
            ids.add(id);
            return id;
        }

        int index = 1;
        while (ids.contains(id + index)) {
            index++;
        }
        ids.add(id + index);
        return id + index;
    }

    public static String toValidId(String id) {
        if (KEYWORDS.contains(id)) {
            id = "_" + id;
        }
        return id.replaceAll("[\\[\\]]", "");
    }


    /**
     * These are java keywords as specified at the following URL.
     * http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#229308
     * Note that false, true, and null are not strictly keywords; they are
     * literal values, but for the purposes of this array, they can be treated
     * as literals.
     */
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays
        .asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
                "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
                "throws", "transient", "true", "try", "void", "volatile", "while"));

}
