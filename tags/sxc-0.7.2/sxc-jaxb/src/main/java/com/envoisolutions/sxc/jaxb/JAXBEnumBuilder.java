/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.envoisolutions.sxc.jaxb;

import static java.beans.Introspector.decapitalize;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.util.XoXMLStreamReader;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.codemodel.JFieldVar;

public class JAXBEnumBuilder {
    private final BuilderContext builderContext;
    private final Class type;
    private final QName xmlRootElement;
    private final QName xmlType;
    private final JDefinedClass jaxbEnumClass;
    private JMethod parseMethod;
    private JVar parseXSR;
    private JVar parseContext;
    private JVar parseValue;
    private JMethod toStringMethod;
    private JVar toStringBean;
    private JVar toStringParameterName;
    private JVar toStringContext;
    private JVar toStringValue;

    public JAXBEnumBuilder(BuilderContext builderContext, Class type, QName xmlRootElement, QName xmlType) {
        this.builderContext = builderContext;
        this.type = type;
        this.xmlRootElement = xmlRootElement;
        this.xmlType = xmlType;

        String className = "sxc." + type.getName() + "JAXB";

        try {
            jaxbEnumClass = builderContext.getCodeModel()._class(className);
            jaxbEnumClass._extends(builderContext.getCodeModel().ref(JAXBEnum.class).narrow(type));
        } catch (JClassAlreadyExistsException e) {
            throw new BuildException(e);
        }

        // INSTANCE variable
        JFieldVar instanceVar = jaxbEnumClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, jaxbEnumClass, "INSTANCE", JExpr._new(jaxbEnumClass));

        // constructor
        JMethod constructor = jaxbEnumClass.constructor(JMod.PUBLIC);
        constructor.body().invoke("super")
                .arg(JExpr.dotclass(builderContext.toJClass(type)))
                .arg(newQName(xmlRootElement))
                .arg(newQName(xmlType));

        // static parse
        JMethod staticParse = jaxbEnumClass.method(JMod.PUBLIC | JMod.STATIC, type, "parse" + type.getSimpleName())._throws(Exception.class);
        JVar xsrVar = staticParse.param(XoXMLStreamReader.class, "reader");
        JVar contextVar = staticParse.param(builderContext.toJClass(RuntimeContext.class), "context");
        JVar value = staticParse.param(String.class, "value");
        staticParse.body()._return(instanceVar.invoke("parse").arg(xsrVar).arg(contextVar).arg(value));

        // static toString
        JMethod staticToString = jaxbEnumClass.method(JMod.PUBLIC | JMod.STATIC, String.class, "toString" + type.getSimpleName())._throws(Exception.class);
        JVar beanVar = staticToString.param(Object.class, "bean");
        JVar parameterNameVar = staticToString.param(String.class, "parameterName");
        contextVar = staticToString.param(builderContext.toJClass(RuntimeContext.class), "context");
        value = staticToString.param(type, decapitalize(type.getSimpleName()));
        staticToString.body()._return(instanceVar.invoke("toString").arg(beanVar).arg(parameterNameVar).arg(contextVar).arg(value));

        // instance parse
        parseMethod = jaxbEnumClass.method(JMod.PUBLIC, type, "parse")._throws(Exception.class);
        parseXSR = parseMethod.param(XoXMLStreamReader.class, "reader");
        parseContext = parseMethod.param(builderContext.toJClass(RuntimeContext.class), "context");
        parseValue = parseMethod.param(String.class, "value");

        // static toString
        toStringMethod = jaxbEnumClass.method(JMod.PUBLIC, String.class, "toString")._throws(Exception.class);
        toStringBean = toStringMethod.param(Object.class, "bean");
        toStringParameterName = toStringMethod.param(String.class, "parameterName");
        toStringContext = toStringMethod.param(builderContext.toJClass(RuntimeContext.class), "context");
        toStringValue = toStringMethod.param(type, decapitalize(type.getSimpleName()));
    }

    public Class getType() {
        return type;
    }

    public QName getXmlRootElement() {
        return xmlRootElement;
    }

    public QName getXmlType() {
        return xmlType;
    }

    public JDefinedClass getJAXBEnumClass() {
        return jaxbEnumClass;
    }

    public JMethod getParseMethod() {
        return parseMethod;
    }

    public JVar getParseXSR() {
        return parseXSR;
    }

    public JVar getParseContext() {
        return parseContext;
    }

    public JVar getParseValue() {
        return parseValue;
    }

    public JMethod getToStringMethod() {
        return toStringMethod;
    }

    public JVar getToStringBean() {
        return toStringBean;
    }

    public JVar getToStringParameterName() {
        return toStringParameterName;
    }

    public JVar getToStringContext() {
        return toStringContext;
    }

    public JVar getToStringValue() {
        return toStringValue;
    }

    private JExpression newQName(QName xmlRootElement) {
        if (xmlRootElement == null) {
            return JExpr._null();
        }
        return JExpr._new(builderContext.toJClass(QName.class))
                .arg(JExpr.lit(xmlRootElement.getNamespaceURI()).invoke("intern"))
                .arg(JExpr.lit(xmlRootElement.getLocalPart()).invoke("intern"));
    }
}