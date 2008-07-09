package com.envoisolutions.sxc.builder.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.envoisolutions.sxc.builder.BuildException;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JStatement;

public class JIfElseBlock implements JStatement {
    private final List<IfCondition> ifConditions = new ArrayList<IfCondition>();
    private JBlock elseBlock;
    private Exception declaredLocation;

    public JIfElseBlock() {
        declaredLocation = new Exception("Declared location");
    }

    public JBlock addCondition(JExpression condition) {
        IfCondition ifCondition = new IfCondition(condition);
        ifConditions.add(ifCondition);
        return ifCondition.block;
    }

    public List<IfCondition> ifConditions() {
        return ifConditions;
    }

    public JBlock _else() {
        if (elseBlock == null) elseBlock = new JBlock();
        return elseBlock;
    }

    public void state(JFormatter f) {
        if (ifConditions.isEmpty()) {
            if (elseBlock != null) {
                throw new BuildException("If else block has an else block but no if blocks", declaredLocation);
            }
            // totally empty statement
            return;
        }

        for (Iterator<IfCondition> iterator = ifConditions.iterator(); iterator.hasNext();) {
            IfCondition ifCondition = iterator.next();

            // avoid adding partnenthese for expressions are automatically wrapped with parentheses
            if (alreadyHasParentheses(ifCondition.test)) {
                f.p("if ").g(ifCondition.test);
            } else {
                f.p("if (").g(ifCondition.test).p(")");
            }
            f.g(ifCondition.block);

            if (iterator.hasNext()) {
                f.p("else ");
            }
        }
        if (elseBlock != null) {
            f.p("else ").g(elseBlock);
        }
        f.nl();
    }

    private boolean alreadyHasParentheses(JExpression condition) {
        String conditionType = condition.getClass().getName();
        // Unary and binary operators automatically add parentheses... to bad these classes are private
        return "com.sun.codemodel.JOp$UnaryOp".equals(conditionType) || "com.sun.codemodel.JOp$BinaryOp".equals(conditionType);
    }

    public static class IfCondition {
        private final JExpression test;
        private final JBlock block = new JBlock();

        private IfCondition(JExpression test) {
            this.test = test;
        }

        public JExpression test() {
            return test;
        }

        public JBlock body() {
            return block;
        }
    }
}