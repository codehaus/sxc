package com.envoisolutions.sxc.jaxb.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.envoisolutions.sxc.builder.BuildException;
import com.sun.xml.bind.api.AccessorException;
import com.sun.xml.bind.v2.model.runtime.RuntimeAttributePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeClassInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElement;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeElementPropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeEnumLeafInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeLeafInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeReferencePropertyInfo;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeInfoSet;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeRef;
import com.sun.xml.bind.v2.model.runtime.RuntimeValuePropertyInfo;
import com.sun.xml.bind.v2.runtime.Transducer;
import com.sun.xml.bind.v2.runtime.reflect.Accessor;

public class RiModelBuilder {
    public Model createModel(RuntimeTypeInfoSet runtimeTypeInfoSet) {
        Model model = new Model();
        for (RuntimeClassInfo runtimeClassInfo : runtimeTypeInfoSet.beans().values()) {
            addBean(model, runtimeClassInfo);
        }

        for (RuntimeEnumLeafInfo runtimeEnumLeafInfo : runtimeTypeInfoSet.enums().values()) {
            addBean(model, runtimeEnumLeafInfo);
        }
        return model;
    }

    private Bean addBean(Model model, RuntimeClassInfo runtimeClassInfo) {
        Bean bean = model.getBean(runtimeClassInfo.getClazz());

        if (bean == null) {
            bean = new Bean(model, runtimeClassInfo.getClazz());

            // bean must be added to model before initializing
            model.addBean(bean);

            initBean(model, bean, runtimeClassInfo);
        }

        return bean;
    }

    private Bean addBean(Model model, RuntimeEnumLeafInfo runtimeEnumLeafInfo) {
        // compiler can't handle the wacy over loaded methods in the jaxb ri
        // so we convert it to a simpler type
        RuntimeLeafInfo leafInfo = runtimeEnumLeafInfo;

        Bean bean = model.getBean(leafInfo.getClazz());

        if (bean == null) {
            if (Modifier.isAbstract(leafInfo.getClazz().getModifiers())) {
                return null;
            }

            bean = new Bean(model, leafInfo.getClazz());

            // bean must be added to model before initializing
            model.addBean(bean);

            initBean(bean, runtimeEnumLeafInfo);
        }

        return bean;
    }

    private void initBean(Model model, Bean bean, RuntimeClassInfo runtimeClassInfo) {
        bean.setRootElementName(runtimeClassInfo.getElementName());
        bean.setSchemaTypeName(runtimeClassInfo.getTypeName());

        for (RuntimePropertyInfo runtimePropertyInfo : runtimeClassInfo.getProperties()) {
            Property property = createProperty(bean, runtimePropertyInfo);
            bean.addProperty(property);
        }

        Bean baseClass = null;
        RuntimeClassInfo baseClassInfo = runtimeClassInfo.getBaseClass();
        if (baseClassInfo != null) {
            baseClass = model.getBean(baseClassInfo.getClazz());
            if (baseClass == null) {
                baseClass = addBean(model, baseClassInfo);
            }
        }
        bean.setBaseClass(baseClass);
    }

    @SuppressWarnings({"unchecked"})
    private void initBean(Bean bean, RuntimeEnumLeafInfo runtimeEnumLeafInfo) {
        bean.setRootElementName(runtimeEnumLeafInfo.getElementName());
        bean.setSchemaTypeName(runtimeEnumLeafInfo.getTypeName());

        Class<?> type = bean.getType();
        Enum[] enumValues;
        try {
            Method method = type.getMethod("values");
            enumValues = (Enum[]) method.invoke(null);
        } catch (Exception e) {
            throw new BuildException("Class is not an enumeration " + type.getName());
        }

        if (enumValues.length == 0) {
            throw new BuildException("Enum contains no values " + type.getName());
        }

        Transducer transducer = (Transducer) runtimeEnumLeafInfo;
        for (Enum enumValue : enumValues) {
            String enumText;
            try {
                enumText = transducer.print(enumValue).toString();
            } catch (AccessorException e) {
                throw new BuildException(e);
            }
            bean.getEnumMap().put(enumValue, enumText);
        }
    }

    private Property createProperty(Bean bean, RuntimePropertyInfo runtimePropertyInfo) {
        Property property = new Property(bean, runtimePropertyInfo.getName());

        property.setType(runtimePropertyInfo.getRawType());
        property.setComponentType(runtimePropertyInfo.getIndividualType());

        property.setCollection(runtimePropertyInfo.isCollection());

        if (runtimePropertyInfo.getAdapter() != null) {
            property.setAdapterType(runtimePropertyInfo.getAdapter().adapterType);
        }

        Accessor accessor = runtimePropertyInfo.getAccessor();

        // if we have an AdaptedAccessor wrapper, strip off wrapper but preserve the name of the adapter class
//        Class<XmlAdapter> xmlAdapterClass = null;
        if ("com.sun.xml.bind.v2.runtime.reflect.AdaptedAccessor".equals(accessor.getClass().getName())) {
            try {
//                // fields on AdaptedAccessor are private so use set accessible to grab the values
//                Field adapterClassField = accessor.getClass().getDeclaredField("adapter");
//                adapterClassField.setAccessible(true);
//                xmlAdapterClass = (Class<XmlAdapter>) adapterClassField.get(accessor);

                Field coreField = accessor.getClass().getDeclaredField("core");
                coreField.setAccessible(true);
                accessor = (Accessor) coreField.get(accessor);
            } catch (Throwable e) {
                throw new BuildException("Unable to access private fields of AdaptedAccessor class", e);
            }
        }

        if (accessor instanceof Accessor.FieldReflection) {
            Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) accessor;
            property.setField(fieldReflection.f);
        } else if (accessor instanceof Accessor.GetterSetterReflection) {
            Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) accessor;
            property.setGetter(getterSetterReflection.getter);
            property.setSetter(getterSetterReflection.setter);
        } else {
            throw new BuildException("Unknown property accessor type '" + accessor.getClass().getName() + "' for property " + property);
        }

        if (runtimePropertyInfo instanceof RuntimeAttributePropertyInfo) {
            RuntimeAttributePropertyInfo attributeProperty = (RuntimeAttributePropertyInfo) runtimePropertyInfo;
            property.setXmlStyle(Property.XmlStyle.ATTRIBUTE);
            property.setXmlName(attributeProperty.getXmlName());
            property.setRequired(attributeProperty.isRequired());
        } else if (runtimePropertyInfo instanceof RuntimeElementPropertyInfo) {
            RuntimeElementPropertyInfo elementProperty = (RuntimeElementPropertyInfo) runtimePropertyInfo;
            property.setXmlStyle(Property.XmlStyle.ELEMENT);
            property.setXmlName(elementProperty.getXmlName());
            for (RuntimeTypeRef typeRef : elementProperty.getTypes()) {
                ElementMapping elementMapping = createXmlMapping(property, typeRef);
                property.getElementMappings().add(elementMapping);
            }
            property.setRequired(elementProperty.isRequired());
            property.setNillable(elementProperty.isCollectionNillable());
        } else  if (runtimePropertyInfo instanceof RuntimeReferencePropertyInfo) {
            RuntimeReferencePropertyInfo referenceProperty = (RuntimeReferencePropertyInfo) runtimePropertyInfo;
            property.setXmlStyle(Property.XmlStyle.ELEMENT_REF);
            for (RuntimeElement re : referenceProperty.getElements()) {
                RuntimeElementInfo runtimeElement = (RuntimeElementInfo) re;
                ElementMapping elementMapping = createXmlMapping(property, runtimeElement);
                property.getElementMappings().add(elementMapping);
            }
            property.setNillable(referenceProperty.isCollectionNillable());
        } else if (runtimePropertyInfo instanceof RuntimeValuePropertyInfo) {
            property.setXmlStyle(Property.XmlStyle.VALUE);
        } else {
            throw new BuildException("Unknown property type " + runtimePropertyInfo.getClass().getName());
        }

        return property;
    }

    private ElementMapping createXmlMapping(Property property, RuntimeTypeRef runtimeTypeRef) {
        ElementMapping mapping = new ElementMapping(property, runtimeTypeRef.getTagName());

        mapping.setNillable(runtimeTypeRef.isNillable());

        mapping.setComponentType(runtimeTypeRef.getTarget().getType());

        return mapping;
    }

    private ElementMapping createXmlMapping(Property property, RuntimeElementInfo runtimeElement) {
        ElementMapping mapping = new ElementMapping(property, runtimeElement.getElementName());

        mapping.setComponentType(runtimeElement.getContentType().getType());

        return mapping;
    }
}
