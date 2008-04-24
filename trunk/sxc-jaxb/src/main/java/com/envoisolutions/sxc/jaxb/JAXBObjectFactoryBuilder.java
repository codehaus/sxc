package com.envoisolutions.sxc.jaxb;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

public class JAXBObjectFactoryBuilder {
    private final BuilderContext builderContext;
    private final Class type;
    private final JDefinedClass jaxbObjectFactoryClass;
    private JMethod constructor;
    private JInvocation superInvocation;
    private Set<String> dependencies = new TreeSet<String>();
    private JFieldVar rootElements;

    public JAXBObjectFactoryBuilder(BuilderContext builderContext, Class type) {
        this.builderContext = builderContext;
        this.type = type;

        String className = "sxc." + type.getName() + "JAXB";

        try {
            jaxbObjectFactoryClass = builderContext.getCodeModel()._class(className);
            jaxbObjectFactoryClass._extends(builderContext.getCodeModel().ref(JAXBObjectFactory.class).narrow(type));
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        // INSTANCE variable
        jaxbObjectFactoryClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, jaxbObjectFactoryClass, "INSTANCE", JExpr._new(jaxbObjectFactoryClass));

        // constructor
        constructor = jaxbObjectFactoryClass.constructor(JMod.PUBLIC);
        superInvocation = constructor.body().invoke("super").arg(JExpr.dotclass(builderContext.toJClass(type)));

        // Map<QName, JAXBObject> rootElements = new HashMap<QName, JAXBObject>();
        JClass qnameJClass = builderContext.toJClass(QName.class);
        JClass extendsJAXBObjectClass = builderContext.toJClass(Class.class).narrow(builderContext.toJClass(JAXBObject.class).wildcard());
        JClass rootElementsType = builderContext.toJClass(Map.class).narrow(qnameJClass, extendsJAXBObjectClass);
        rootElements = jaxbObjectFactoryClass.field(JMod.PRIVATE | JMod.FINAL, rootElementsType, "rootElements");
        rootElements.init(JExpr._new(builderContext.toJClass(HashMap.class).narrow(qnameJClass, extendsJAXBObjectClass)));

        // public Map<QName, JAXBObject>() getRootElements() { return rootElements; }
        JMethod method = jaxbObjectFactoryClass.method(JMod.PUBLIC, rootElementsType, "getRootElements");
        method.body()._return(rootElements);
    }

    private JExpression newQName(QName xmlRootElement) {
        if (xmlRootElement == null) {
            return JExpr._null();
        }
        return JExpr._new(builderContext.toJClass(QName.class))
                .arg(JExpr.lit(xmlRootElement.getNamespaceURI()).invoke("intern"))
                .arg(JExpr.lit(xmlRootElement.getLocalPart()).invoke("intern"));
    }

    public Class getType() {
        return type;
    }

    public JDefinedClass getJAXBObjectFactoryClass() {
        return jaxbObjectFactoryClass;
    }

    public void addDependency(Class type) {
        if (!type.isEnum()) {
            JAXBObjectBuilder objectBuilder = builderContext.getJAXBObjectBuilder(type);
            if (objectBuilder != null) {
                addDependency(objectBuilder.getJAXBObjectClass());
            }
        } else {
            JAXBEnumBuilder enumBuilder = builderContext.getJAXBEnumBuilder(type);
            if (enumBuilder != null) {
                addDependency(enumBuilder.getJAXBEnumClass());
            }
        }
    }

    public void addDependency(JClass dependency) {
        if (jaxbObjectFactoryClass.fullName().equals(dependency.fullName())) return;

        if (dependencies.add(dependency.fullName())) {
            superInvocation.arg(dependency.dotclass());
        }
    }

    public void addRootElement(QName name, Class type) {
        if (!type.isEnum()) {
            JAXBObjectBuilder objectBuilder = builderContext.getJAXBObjectBuilder(type);
            if (objectBuilder != null) {
                constructor.body().invoke(rootElements, "put")
                        .arg(newQName(name))
                        .arg(objectBuilder.getJAXBObjectClass().dotclass());
            } else {
                JAXBObject jaxbObject = StandardJAXBObjects.jaxbObjectByClass.get(type);
                if (jaxbObject != null) {
                    constructor.body().invoke(rootElements, "put")
                            .arg(newQName(name))
                            .arg(builderContext.dotclass(jaxbObject.getClass()));
                }

            }
        } else {
            JAXBEnumBuilder enumBuilder = builderContext.getJAXBEnumBuilder(type);
            if (enumBuilder != null) {
                constructor.body().invoke(rootElements, "put")
                        .arg(newQName(name))
                        .arg(enumBuilder.getJAXBEnumClass().dotclass());
            }
        }
    }
}