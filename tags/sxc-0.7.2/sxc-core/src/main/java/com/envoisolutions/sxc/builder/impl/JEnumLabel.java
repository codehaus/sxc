package com.envoisolutions.sxc.builder.impl;

import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;

public class JEnumLabel extends JExpressionImpl {
    private final String enumName;

    public JEnumLabel(String enumName) {
        if (enumName == null) throw new NullPointerException("comment is null");
        this.enumName = enumName;
    }

    public void generate(JFormatter f) {
        f.p(enumName);
    }
}