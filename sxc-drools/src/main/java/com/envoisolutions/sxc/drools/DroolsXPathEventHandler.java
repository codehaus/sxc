package com.envoisolutions.sxc.drools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.drools.FactException;
import org.drools.FactHandle;
import org.drools.ObjectFilter;
import org.drools.QueryResults;
import org.drools.WorkingMemory;
import org.drools.base.ClassObjectFilter;
import org.drools.event.WorkingMemoryEventListener;
import org.drools.spi.AgendaFilter;

import com.envoisolutions.sxc.xpath.XPathEvent;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

class DroolsXPathEventHandler extends XPathEventHandler {
    private final WorkingMemory workingMemory;
    private final boolean fireAllInOnMatch;

    public DroolsXPathEventHandler(WorkingMemory workingMemory, boolean fireAllInOnMatch) {
        this.workingMemory = workingMemory;
        this.fireAllInOnMatch = fireAllInOnMatch;
    }

    @Override
    public void onMatch(XPathEvent event) throws XMLStreamException {
        workingMemory.insert(event);
        if (fireAllInOnMatch) {
            workingMemory.fireAllRules();
        }
    }

    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    public boolean isFireAllInOnMatch() {
        return fireAllInOnMatch;
    }

    /* Helpful Delegate Methods off the Working Memory*/
    public void fireAllRules() throws FactException {
        workingMemory.fireAllRules();
    }

    public void fireAllRules(AgendaFilter agendaFilter) throws FactException {
        workingMemory.fireAllRules(agendaFilter);
    }

    public void addEventListener(WorkingMemoryEventListener listener) {
        workingMemory.addEventListener(listener);
    }

    public void setGlobal(String name, Object value) {
        workingMemory.setGlobal(name, value);
    }

    public Object getGlobal(String name) {
        return workingMemory.getGlobal(name);
    }

    public Object getObject(FactHandle handle) throws FactException {
        return workingMemory.getObject(handle);
    }

    public List getObjects(Class objectClass) {
	ObjectFilter objectFilter = new ClassObjectFilter(objectClass);
	List objects = new ArrayList();
	Iterator iter = workingMemory.iterateObjects(objectFilter);
	while (iter.hasNext()) {
	    objects.add(iter.next());
	}
        return objects;
        
    }

    public FactHandle assertObject(Object object) throws FactException {
        return workingMemory.insert(object);
    }

    public QueryResults getQueryResults(String query) {
        return workingMemory.getQueryResults(query);
    }

    public FactHandle assertObject(Object object, boolean dynamic) throws FactException {
        return workingMemory.insert(object, dynamic);
    }

    public void retractObject(FactHandle handle) throws FactException {
        workingMemory.retract(handle);
    }

    public void modifyObject(FactHandle handle, Object object) throws FactException {
        workingMemory.update(handle, object);
    }
}
