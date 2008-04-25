package com.envoisolutions.sxc.jaxb.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.Callable;
import java.beans.Introspector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;

import com.envoisolutions.sxc.builder.BuildException;
import com.envoisolutions.sxc.jaxb.JAXBModelFactory;
import com.envoisolutions.sxc.jaxb.JavaUtils;
import com.sun.xml.bind.api.AccessorException;
import com.sun.xml.bind.v2.ContextFactory;
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
import com.sun.xml.bind.v2.model.core.WildcardMode;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.bind.v2.runtime.Transducer;
import com.sun.xml.bind.v2.runtime.reflect.Accessor;

public class RiModelBuilder {
    private final JAXBContextImpl context;
    private final Model model;

    public RiModelBuilder(Map<String, Object> properties, Class... classes) throws JAXBException {
        context = (JAXBContextImpl) ContextFactory.createContext(classes, properties);

        RuntimeTypeInfoSet runtimeTypeInfoSet = JAXBModelFactory.create(context, classes);

        model = new Model();

        for (RuntimeEnumLeafInfo runtimeEnumLeafInfo : runtimeTypeInfoSet.enums().values()) {
            addEnum(model, runtimeEnumLeafInfo);
        }

        for (RuntimeClassInfo runtimeClassInfo : runtimeTypeInfoSet.beans().values()) {
            addBean(model, runtimeClassInfo);
        }

        for (Class clazz : classes) {
            if (clazz.isAnnotationPresent(XmlRegistry.class)) {
                addXmlRegistry(clazz);
            }
        }
    }

    public Model getModel() {
        return model;
    }

    public Callable<JAXBContext> getContext() {
        return new Callable<JAXBContext>() {
            public JAXBContext call() throws Exception {
                return context;
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    private EnumInfo addEnum(Model model, RuntimeEnumLeafInfo runtimeEnumLeafInfo) {
        // compiler can't handle the wacky over loaded methods in the jaxb ri
        // so we convert it to a simpler type
        RuntimeLeafInfo leafInfo = runtimeEnumLeafInfo;

        EnumInfo enumInfo = model.getEnum(leafInfo.getClazz());

        if (enumInfo == null) {
            if (Modifier.isAbstract(leafInfo.getClazz().getModifiers())) {
                return null;
            }

            enumInfo = new EnumInfo(model, leafInfo.getClazz());
            enumInfo.setRootElementName(runtimeEnumLeafInfo.getElementName());
            enumInfo.setSchemaTypeName(runtimeEnumLeafInfo.getTypeName());

            Class<?> type = enumInfo.getType();
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
                enumInfo.getEnumMap().put(enumValue, enumText);
            }

            model.addEnum(enumInfo);
        }

        return enumInfo;
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

    private void initBean(Model model, Bean bean, RuntimeClassInfo runtimeClassInfo) {
        bean.setRootElementName(runtimeClassInfo.getElementName());
        bean.setSchemaTypeName(runtimeClassInfo.getTypeName());

        for (RuntimePropertyInfo runtimePropertyInfo : runtimeClassInfo.getProperties()) {
            Property property = createProperty(bean, runtimePropertyInfo);
            bean.addProperty(property);
        }

        if (runtimeClassInfo.declaresAttributeWildcard()) {
            Accessor anyAttributeAccessor = runtimeClassInfo.getAttributeWildcard();
            Property anyAttributeProperty;

            // if we have an AdaptedAccessor wrapper, strip off wrapper but preserve the name of the adapter class
//        Class<XmlAdapter> xmlAdapterClass = null;
            if ("com.sun.xml.bind.v2.runtime.reflect.AdaptedAccessor".equals(anyAttributeAccessor.getClass().getName())) {
                try {
    //                // fields on AdaptedAccessor are private so use set accessible to grab the values
    //                Field adapterClassField = accessor.getClass().getDeclaredField("adapter");
    //                adapterClassField.setAccessible(true);
    //                xmlAdapterClass = (Class<XmlAdapter>) adapterClassField.get(accessor);

                    Field coreField = anyAttributeAccessor.getClass().getDeclaredField("core");
                    coreField.setAccessible(true);
                    anyAttributeAccessor = (Accessor) coreField.get(anyAttributeAccessor);
                } catch (Throwable e) {
                    throw new BuildException("Unable to access private fields of AdaptedAccessor class", e);
                }
            }

            if (anyAttributeAccessor instanceof Accessor.FieldReflection) {
                Accessor.FieldReflection fieldReflection = (Accessor.FieldReflection) anyAttributeAccessor;
                anyAttributeProperty = new Property(bean, fieldReflection.f.getName());
                anyAttributeProperty.setField(fieldReflection.f);
                anyAttributeProperty.setType(fieldReflection.f.getGenericType());
            } else if (anyAttributeAccessor instanceof Accessor.GetterSetterReflection) {
                Accessor.GetterSetterReflection getterSetterReflection = (Accessor.GetterSetterReflection) anyAttributeAccessor;

                if (getterSetterReflection.getter != null) {
                    String propertyName = getterSetterReflection.getter.getName();
                    if (propertyName.startsWith("get")) {
                        propertyName = Introspector.decapitalize(propertyName.substring(3));
                    } else if (propertyName.startsWith("is")) {
                        propertyName = Introspector.decapitalize(propertyName.substring(2));
                    }
                    anyAttributeProperty = new Property(bean, propertyName);
                    anyAttributeProperty.setType(getterSetterReflection.getter.getGenericReturnType());
                } else if (getterSetterReflection.setter != null) {
                    String propertyName = getterSetterReflection.setter.getName();
                    if (propertyName.startsWith("set")) {
                        propertyName = Introspector.decapitalize(propertyName.substring(3));
                    }
                    anyAttributeProperty = new Property(bean, propertyName);
                    anyAttributeProperty.setType(getterSetterReflection.setter.getGenericParameterTypes()[0]);
                } else {
                    throw new BuildException("Any attribute property for class " + bean + " does not have a getter or setter method");
                }

                anyAttributeProperty.setGetter(getterSetterReflection.getter);
                anyAttributeProperty.setSetter(getterSetterReflection.setter);
            } else {
                throw new BuildException("Unknown property accessor type '" + anyAttributeAccessor.getClass().getName() + "' for class " + bean);
            }            

            anyAttributeProperty.setXmlStyle(Property.XmlStyle.ATTRIBUTE);
            anyAttributeProperty.setComponentType(String.class);
            anyAttributeProperty.setXmlAny(true);
            bean.addProperty(anyAttributeProperty);
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
                ElementMapping elementMapping;
                if (re instanceof RuntimeElementInfo) {
                    RuntimeElementInfo runtimeElement = (RuntimeElementInfo) re;
                    elementMapping = createXmlMapping(property, runtimeElement);
                } else {
                    RuntimeClassInfo runtimeClassInfo = (RuntimeClassInfo) re;
                    elementMapping = createXmlMapping(property, runtimeClassInfo);
                }
                property.getElementMappings().add(elementMapping);
            }
            property.setNillable(referenceProperty.isCollectionNillable());
            property.setXmlAny(referenceProperty.getWildcard() != null);
            property.setLax(referenceProperty.getWildcard() == WildcardMode.LAX);
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

    private ElementMapping createXmlMapping(Property property, RuntimeClassInfo runtimeClassInfo) {
        ElementMapping mapping = new ElementMapping(property, runtimeClassInfo.getElementName());

        mapping.setComponentType(runtimeClassInfo.getClazz());

        return mapping;
    }

    private void addXmlRegistry(Class clazz) {
        ObjectFactory objectFactory = new ObjectFactory(model, clazz);
        model.getObjectFactories().add(objectFactory);
        String pkgNamespace = null;
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(XmlElementDecl.class)) {
                Class<?> type = null;
                if (method.getParameterTypes().length > 0) {
                    // according to the JAXB team it is more reliable to determine referenced class type
                    // from the method parameter type instead of the return type
                    type = method.getParameterTypes()[0];
                } else if (method.getReturnType().equals(JAXBElement.class)) {
                    // return type should be a parameterized JAXBElement
                    Type returnType = method.getGenericReturnType();
                    if (returnType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) returnType;
                        if (parameterizedType.getActualTypeArguments().length > 0) {
                            Type typeArg = parameterizedType.getActualTypeArguments()[0];
                            type = JavaUtils.toClass(typeArg);
                        }
                    }
                }

                if (type != null) {
                    XmlElementDecl xmlElementDecl = method.getAnnotation(XmlElementDecl.class);
                    String name = xmlElementDecl.name();
                    String namespace = xmlElementDecl.namespace();
                    if ("##default".equals(namespace)) {
                        if (pkgNamespace == null) {
                            Package pkg = clazz.getPackage();
                            if (pkg != null) {
                                XmlSchema annotation = pkg.getAnnotation(XmlSchema.class);
                                if (annotation != null) {
                                    pkgNamespace = annotation.namespace();
                                }
                            }
                            if (pkgNamespace == null || "##default".equals(pkgNamespace)) {
                                pkgNamespace = "";
                            }
                        }
                        namespace = pkgNamespace;
                    }

                    objectFactory.getRootElements().put(new QName(namespace, name), type);

                    Bean bean = model.getBean(type);
                    if (bean != null) {
                        objectFactory.getDependencies().add(bean);
                    }
                }
            } else if(method.getName().startsWith("create")) {
                Bean bean = model.getBean(method.getReturnType());
                if (bean != null) {
                    objectFactory.getDependencies().add(bean);
                }
            }
        }
    }
}
