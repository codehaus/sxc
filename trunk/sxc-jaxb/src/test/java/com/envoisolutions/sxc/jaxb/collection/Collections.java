package com.envoisolutions.sxc.jaxb.collection;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;

@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class Collections {
    public static final Collection<String> INITIALIZED_FIELD = new CustomCollection<String>();
    public static final Collection<String> INITIALIZED_PROPERTY = new CustomCollection<String>();
    public static final Collection<String> FINAL_PROPERTY = new CustomCollection<String>();

    //
    // Public fields

    // intefaces type fields
    @XmlElement(name = "collection-field")
    public Collection<String> collectionField;
    public List<String> listField;
    public Set<String> setField;
    public SortedSet<String> sortedSetField;
    public Queue<String> queueField;

    // instances type fields
    public LinkedHashSet<String> linkedHashSetField;
    public LinkedList<String> linkedListField;
    public CustomCollection<String> customCollectionField;

    // field has an initial value and should not be reassigned
    public Collection<String> initializedField = INITIALIZED_FIELD;

    // field is final and JaxB impl will fill in a value later
    public final Collection<String> finalField = null;

    // this collection can not be created by JaxB because it does not have a default constructor
    public UncreatableCollection<String> uncreatableCollectionField;

    // this collection interface can not be created by JaxB because it does not have a "known" implementations
    public UnknownCollection<String> unknownCollectionField;

    //
    // Properties

    // intefaces type properties
    private Collection<String> collectionProperty;
    private List<String> listProperty;
    private Set<String> setProperty;
    private SortedSet<String> sortedSetProperty;
    private Queue<String> queueProperty;

    // instances type properties
    private LinkedHashSet<String> linkedHashSetProperty;
    private LinkedList<String> linkedListProperty;
    private CustomCollection<String> customCollectionProperty;

    // property has an initial value and should not be reassigned
    private Collection<String> initializedProperty = INITIALIZED_PROPERTY;

    // property only has a getter so code can not be generated for that calls a setter
    private final Collection<String> finalProperty = FINAL_PROPERTY;

    // this collection can not be created by JaxB because it does not have a default constructor
    private UncreatableCollection<String> uncreatableCollectionProperty;

    // this collection interface can not be created by JaxB because it does not have a "known" implementations
    private UnknownCollection<String> unknownCollectionProperty;

    @XmlElement(name = "collection-property")
    public Collection<String> getCollectionProperty() {
        return collectionProperty;
    }

    public void setCollectionProperty(Collection<String> collectionProperty) {
        this.collectionProperty = collectionProperty;
    }

    public List<String> getListProperty() {
        return listProperty;
    }

    public void setListProperty(List<String> listProperty) {
        this.listProperty = listProperty;
    }

    public Set<String> getSetProperty() {
        return setProperty;
    }

    public void setSetProperty(Set<String> setProperty) {
        this.setProperty = setProperty;
    }

    public SortedSet<String> getSortedSetProperty() {
        return sortedSetProperty;
    }

    public void setSortedSetProperty(SortedSet<String> sortedSetProperty) {
        this.sortedSetProperty = sortedSetProperty;
    }

    public Queue<String> getQueueProperty() {
        return queueProperty;
    }

    public void setQueueProperty(Queue<String> queueProperty) {
        this.queueProperty = queueProperty;
    }

    public LinkedHashSet<String> getLinkedHashSetProperty() {
        return linkedHashSetProperty;
    }

    public void setLinkedHashSetProperty(LinkedHashSet<String> linkedHashSetProperty) {
        this.linkedHashSetProperty = linkedHashSetProperty;
    }

    public LinkedList<String> getLinkedListProperty() {
        return linkedListProperty;
    }

    public void setLinkedListProperty(LinkedList<String> linkedListProperty) {
        this.linkedListProperty = linkedListProperty;
    }

    public CustomCollection<String> getCustomCollectionProperty() {
        return customCollectionProperty;
    }

    public void setCustomCollectionProperty(CustomCollection<String> customCollectionProperty) {
        this.customCollectionProperty = customCollectionProperty;
    }

    public Collection<String> getInitializedProperty() {
        return initializedProperty;
    }

    public void setInitializedProperty(Collection<String> initializedProperty) {
        this.initializedProperty = initializedProperty;
    }

    @XmlElement
    public Collection<String> getFinalProperty() {
        return finalProperty;
    }

    public UncreatableCollection<String> getUncreatableCollectionProperty() {
        return uncreatableCollectionProperty;
    }

    public void setUncreatableCollectionProperty(UncreatableCollection<String> uncreatableCollectionProperty) {
        this.uncreatableCollectionProperty = uncreatableCollectionProperty;
    }

    public UnknownCollection<String> getUnknownCollectionProperty() {
        return unknownCollectionProperty;
    }

    public void setUnknownCollectionProperty(UnknownCollection<String> unknownCollectionProperty) {
        this.unknownCollectionProperty = unknownCollectionProperty;
    }

    public static final class CustomCollection<E> extends LinkedHashSet<E> {
    }

    public static final class UncreatableCollection<E> extends LinkedHashSet<E> {
        @SuppressWarnings({"UnusedDeclaration"})
        public UncreatableCollection(int ignored) {
        }
    }

    public static interface UnknownCollection<E> extends Collection<E> {
    }

    public static final class UnknownCollectionImpl<E> extends LinkedHashSet<E> implements UnknownCollection<E> {
    }

}