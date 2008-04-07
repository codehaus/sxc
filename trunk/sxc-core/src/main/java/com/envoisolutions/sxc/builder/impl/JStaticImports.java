package com.envoisolutions.sxc.builder.impl;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;

import com.envoisolutions.sxc.builder.BuildException;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCommentPart;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JVar;

public class JStaticImports {
    public static JStaticImports getStaticImports(JDefinedClass definedClass) {
        JDocComment docComment = getDocComment(definedClass);
        if (docComment instanceof JDocCommentWrapper) {
            JDocCommentWrapper wrapper = (JDocCommentWrapper) docComment;
            return wrapper.getStaticImports();
        }

        JStaticImports staticImports = new JStaticImports();
        JDocCommentWrapper wrapper = new JDocCommentWrapper(staticImports, definedClass, docComment);
        setDocComment(definedClass, wrapper);
        return staticImports;
    }

    private static JDocComment getDocComment(JDefinedClass definedClass) {
        try {
            Field jdocField = JDefinedClass.class.getDeclaredField("jdoc");
            jdocField.setAccessible(true);
            return (JDocComment) jdocField.get(definedClass);
        } catch (Exception e) {
            throw new BuildException("Unable to hack into JDefinedClass to add staic imports");
        }
    }

    private static void setDocComment(JDefinedClass definedClass, JDocComment value) {
        try {
            Field jdocField = JDefinedClass.class.getDeclaredField("jdoc");
            jdocField.setAccessible(true);
            jdocField.set(definedClass, value);
        } catch (Exception e) {
            throw new BuildException("Unable to hack into JDefinedClass to add staic imports");
        }
    }

    private final TreeSet<String> staticImports = new TreeSet<String>();

    public void addStaticImport(String staticImport) {
        staticImports.add(staticImport);
    }

    public TreeSet<String> getStaticImports() {
        return staticImports;
    }

    public void generate(JFormatter f) {
        for (String staticImport : staticImports) {
            f.p("import static " + staticImport + ";").nl();
        }
        if (!staticImports.isEmpty()) {
            f.nl();
        }
    }

    private static class JDocCommentWrapper extends JDocComment {
        private final JStaticImports staticImports;
        private final JDefinedClass definedClass;
        private JDocComment delegate;

        private JDocCommentWrapper(JStaticImports staticImports, JDefinedClass definedClass, JDocComment delegate) {
            super(null);
            this.staticImports = staticImports;
            this.definedClass = definedClass;
            this.delegate = delegate;
        }

        public JStaticImports getStaticImports() {
            return staticImports;
        }

        public JDefinedClass getDefinedClass() {
            return definedClass;
        }

        public JDocComment getDelegate() {
            if (delegate == null) {
                delegate = new JDocComment(definedClass.owner());
            }
            return delegate;
        }

        public void generate(JFormatter f) {
            staticImports.generate(f);
            if (delegate != null) {
                delegate.generate(f);
            }
        }

        public boolean add(Object o) {
            return getDelegate().add(o);
        }

        public void trimToSize() {
            getDelegate().trimToSize();
        }

        public void ensureCapacity(int minCapacity) {
            getDelegate().ensureCapacity(minCapacity);
        }

        public int size() {
            return getDelegate().size();
        }

        public boolean isEmpty() {
            return getDelegate().isEmpty();
        }

        public boolean contains(Object elem) {
            return getDelegate().contains(elem);
        }

        public int indexOf(Object elem) {
            return getDelegate().indexOf(elem);
        }

        public int lastIndexOf(Object elem) {
            return getDelegate().lastIndexOf(elem);
        }

        public Object[] toArray() {
            return getDelegate().toArray();
        }

        public <T> T[] toArray(T[] a) {
            return getDelegate().toArray(a);
        }

        public Object get(int index) {
            return getDelegate().get(index);
        }

        public Object set(int index, Object element) {
            return getDelegate().set(index, element);
        }

        public void add(int index, Object element) {
            getDelegate().add(index, element);
        }

        public Object remove(int index) {
            return getDelegate().remove(index);
        }

        public boolean remove(Object o) {
            return getDelegate().remove(o);
        }

        public void clear() {
            getDelegate().clear();
        }

        public boolean addAll(Collection<? extends Object> c) {
            return getDelegate().addAll(c);
        }

        public boolean addAll(int index, Collection<? extends Object> c) {
            return getDelegate().addAll(index, c);
        }

        public Iterator<Object> iterator() {
            return getDelegate().iterator();
        }

        public ListIterator<Object> listIterator() {
            return getDelegate().listIterator();
        }

        public ListIterator<Object> listIterator(int index) {
            return getDelegate().listIterator(index);
        }

        public List<Object> subList(int fromIndex, int toIndex) {
            return getDelegate().subList(fromIndex, toIndex);
        }

        public boolean equals(Object o) {
            return getDelegate().equals(o);
        }

        public int hashCode() {
            return getDelegate().hashCode();
        }

        public boolean containsAll(Collection<?> c) {
            return getDelegate().containsAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return getDelegate().removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return getDelegate().retainAll(c);
        }

        public String toString() {
            return getDelegate().toString();
        }

        public JDocComment append(Object o) {
            return getDelegate().append(o);
        }

        public JCommentPart addParam(String param) {
            return getDelegate().addParam(param);
        }

        public JCommentPart addParam(JVar param) {
            return getDelegate().addParam(param);
        }

        public JCommentPart addThrows(Class exception) {
            return getDelegate().addThrows(exception);
        }

        public JCommentPart addThrows(JClass exception) {
            return getDelegate().addThrows(exception);
        }

        public JCommentPart addReturn() {
            return getDelegate().addReturn();
        }

        public JCommentPart addDeprecated() {
            return getDelegate().addDeprecated();
        }

        public Map<String, String> addXdoclet(String name) {
            return getDelegate().addXdoclet(name);
        }

        public Map<String, String> addXdoclet(String name, Map<String, String> attributes) {
            return getDelegate().addXdoclet(name, attributes);
        }

        public Map<String, String> addXdoclet(String name, String attribute, String value) {
            return getDelegate().addXdoclet(name, attribute, value);
        }
    }
}