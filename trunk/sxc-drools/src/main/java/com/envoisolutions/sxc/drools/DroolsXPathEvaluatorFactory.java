package com.envoisolutions.sxc.drools;

import com.envoisolutions.sxc.xpath.XPathBuilder;
import com.envoisolutions.sxc.xpath.XPathEvaluator;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.Pattern;
import org.drools.rule.EvalCondition;
import org.drools.rule.GroupElement;
import org.drools.rule.LiteralConstraint;
import org.drools.rule.Rule;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DroolsXPathEvaluatorFactory {

    private final Map<String, String> prefixes = new HashMap<String, String>();
    private InputStream packageStream = null;
    private RuleBase ruleBase = null;
    private WorkingMemory workingMemory = null;
    private boolean fireAllInOnMatch = true;
    private XPathEventHandler xPathEventHandler = null;

    public void addPrefix(String prefix, String namespace) {
        prefixes.put(prefix, namespace);
    }

    public void setPackageStream(InputStream packageStream) {
        this.packageStream = packageStream;
    }

    public RuleBase getRuleBase() {
        return ruleBase;
    }

    public void setRuleBase(RuleBase ruleBase) {
        this.ruleBase = ruleBase;
    }

    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    public void setWorkingMemory(WorkingMemory workingMemory) {
        this.workingMemory = workingMemory;
    }

    public boolean isFireAllInOnMatch() {
        return fireAllInOnMatch;
    }

    public void setFireAllInOnMatch(boolean fireAllInOnMatch) {
        this.fireAllInOnMatch = fireAllInOnMatch;
    }

    public XPathEventHandler getXPathEventHandler() {
        return xPathEventHandler;
    }

    public void setXPathEventHandler(XPathEventHandler xPathEventHandler) {
        this.xPathEventHandler = xPathEventHandler;
    }

    public XPathEvaluator create() throws Exception {
        XPathBuilder builder = new XPathBuilder();
        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            builder.addPrefix(entry.getKey(), entry.getValue());
        }
        if (workingMemory == null && ruleBase == null && packageStream == null) {
            throw new RuntimeException("You must set either the WorkingMemory, RuleBase, or PackageStream properties.");
        }
        if (packageStream != null) {
            // process package stream
            ruleBase = RuleBaseFactory.newRuleBase();
            final PackageBuilder packageBuilder = new PackageBuilder();
            packageBuilder.addPackageFromDrl(new InputStreamReader(packageStream));
            ruleBase.addPackage(packageBuilder.getPackage());
            workingMemory = ruleBase.newStatefulSession(true);
        } else {
            if (ruleBase != null) {
                // process rule base
                workingMemory = ruleBase.newStatefulSession(true);
            } else {
                // process working memory
                ruleBase = workingMemory.getRuleBase();
            }
        }
        assert (ruleBase != null);
        assert (workingMemory != null);

        final org.drools.rule.Package[] packages = ruleBase.getPackages();
        final Set<String> xPathStrings = new HashSet<String>();
        for (final org.drools.rule.Package aPackage : packages) {
            final Rule[] rules = aPackage.getRules();
            for (final Rule rule : rules) {
                final GroupElement lhs = rule.getLhs();
                final List list = lhs.getChildren();
                processList(list, xPathStrings);
            }

        }
        if (xPathEventHandler == null) {
            // allow them to pass in their own
            xPathEventHandler = new DroolsXPathEventHandler(workingMemory, fireAllInOnMatch);
        }
        for (String xPathString : xPathStrings) {
            builder.listen(xPathString, xPathEventHandler);
        }

        return builder.compile();
    }

    private void processList(List list, Set<String> xPathStrings) {
        for (Object ruleElement : list) {
            if (ruleElement instanceof Pattern) {
                final Pattern pattern = (Pattern) ruleElement;
                processPattern(pattern, xPathStrings);
            } else if (ruleElement instanceof GroupElement) {
                final GroupElement groupElement = (GroupElement) ruleElement;
                final List children = groupElement.getChildren();
                processList(children, xPathStrings);
            } else if (ruleElement instanceof EvalCondition) {
                // ignore
            } else {
                throw new RuntimeException("Could not process rule element: " + ruleElement);
            }
        }
    }

    private void processPattern(Pattern pattern, Set<String> xPathStrings) {
        final List constraints = pattern.getConstraints();
        for (Object constraint : constraints) {
            final LiteralConstraint literalConstraint = (LiteralConstraint) constraint;
            if (patternContainsXPathEvent(literalConstraint)) {
                final String fieldValue = (String) literalConstraint.getField().getValue();
                xPathStrings.add(fieldValue);
            }
        }
    }

    /**
     * TODO I'm well aware that this is a horrid hack, but after stepping through the debugger and looking at the
     * TODO source code, the class is there, but it's private, and not being exposed anywhere.
     *
     * @param literalConstraint
     * @return
     */
    private boolean patternContainsXPathEvent(LiteralConstraint literalConstraint) {
        return literalConstraint.toString().indexOf("com.envoisolutions.sxc.xpath.XPathEvent") > -1;
    }

}
