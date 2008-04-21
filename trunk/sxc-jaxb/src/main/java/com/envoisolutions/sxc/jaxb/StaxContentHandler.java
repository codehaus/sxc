package com.envoisolutions.sxc.jaxb;

import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class StaxContentHandler implements ContentHandler {
    public static interface StaxParser {
        public void parse(XMLEventReader reader);
    }

    private static final EOF EOF = new EOF();
    private StaxParser staxParser;
    private long timeout;
    private final SynchronousQueue<XMLEvent> queue = new SynchronousQueue<XMLEvent>();
    private boolean destroyed;
    private final AtomicBoolean shouldClose = new AtomicBoolean(false);

    private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    private final LinkedList<List<Namespace>> namespaceStack = new LinkedList<List<Namespace>>();
    private final LinkedList<NamespaceContext> namespaceContext = new LinkedList<NamespaceContext>();
    private List<Namespace> workingNamespaces = new ArrayList<Namespace>();

    private Locator locator;
    private Thread worker;

    public StaxContentHandler() {
        this(null, 5000);
    }
    public StaxContentHandler(StaxParser staxParser) {
        this(staxParser, 5000);
    }

    private StaxContentHandler(StaxParser staxParser, long timeout) {
        this.staxParser = staxParser;
        this.timeout = timeout;

        eventFactory.setLocation(new SaxLocation());
    }

    public StaxParser getStaxParser() {
        return staxParser;
    }

    public void setStaxParser(StaxParser staxParser) {
        this.staxParser = staxParser;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void startDocument() throws SAXException {
        if (destroyed) {
            throw new SAXException("StaxContentHandler has been destroyed");
        }

        if (worker != null) {
            throw new SAXException("Worker already started");
        }

        worker = new Thread(new StaxParserWorker(staxParser, timeout, queue, shouldClose), "StaxParser-");
        worker.setDaemon(true);
        worker.start();

        XMLEvent event = eventFactory.createStartDocument();
        postEvent(event);
    }

    private void postEvent(XMLEvent event) throws SAXException {
        if (destroyed) {
            return;
        } else if (worker == null) {
            throw new SAXException("No worker thread");
        } else if (!worker.isAlive()) {
            throw new SAXException("Worker thread died");
        }

        if (shouldClose.get()) {
            destroy();
            return;
        }

        try {
            queue.offer(event, timeout, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            // worker thread is timed out... just kill it
            worker.interrupt();
            worker = null;
            destroyed = true;
            throw new SAXException((Exception) e);
        }
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;

        // do we have a live worker?
        if (worker == null) {
            return;
        }
        if (!worker.isAlive()) {
            worker = null;
        }

        // worker will not exit until it receives an EOF
        try {
            queue.offer(EOF, timeout, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            worker.interrupt();
            worker = null;
            return;
        }

        // wait 5 seconds for the worker to complete, then interrupt
        try {
            worker.join(timeout);
        } catch (InterruptedException e) {
            worker.interrupt();
        }

        // worker is dead
        worker = null;
    }

    public void endDocument() throws SAXException {
        if (destroyed) return;

        XMLEvent event = eventFactory.createEndDocument();
        postEvent(event);
        destroy();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (destroyed) return;

        Namespace namespace;
        if (prefix.length() == 0) {
            namespace = eventFactory.createNamespace(uri);
        } else {
            namespace = eventFactory.createNamespace(prefix, uri);
        }

        workingNamespaces.add(namespace);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        // ignored... we pop all working namespaces at the end of the element
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (destroyed) return;

        List<Attribute> attributes = new ArrayList<Attribute>(atts.getLength());
        for (int i = 0; i < atts.getLength(); i++) {
            String prefix = getPrefix(atts.getQName(i));
            // ignore namespace declarations - handled below
            if ("xmlns".equals(atts.getQName(i)) || "xmlns".equals(prefix)) {
                continue;
            }
            Attribute attribute = eventFactory.createAttribute(prefix, atts.getURI(i), atts.getLocalName(i), atts.getValue(i));
            attributes.add(attribute);
        }

        for (int i = 0; i < atts.getLength(); i++) {
            Namespace namespace;
            if ("xmlns".equals(atts.getQName(i))) {
                namespace = eventFactory.createNamespace(atts.getValue(i));
            } else {
                namespace = eventFactory.createNamespace(atts.getLocalName(i), atts.getValue(i));
            }
            workingNamespaces.add(namespace);
        }

        StartElement event = eventFactory.createStartElement(getPrefix(qName), uri, localName, attributes.iterator(), workingNamespaces.iterator(), namespaceContext.peek());

        // push the namespaces
        namespaceStack.addFirst(workingNamespaces);
        workingNamespaces = new ArrayList<Namespace>();
        namespaceContext.addFirst(event.getNamespaceContext());

        postEvent(event);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (destroyed) return;

        if (namespaceStack.isEmpty()) {
            throw new SAXException(new IllegalStateException("namespace stack is empty"));
        }

        // pop the namespaces
        List<Namespace> namespaces = namespaceStack.removeFirst();
        workingNamespaces = new ArrayList<Namespace>();
        namespaceContext.removeFirst();

        
        XMLEvent event = eventFactory.createEndElement(getPrefix(qName), uri, localName, namespaces.iterator());
        postEvent(event);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (destroyed) return;

        XMLEvent event = eventFactory.createCharacters(new String(ch, start, length));
        postEvent(event);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (destroyed) return;

        XMLEvent event = eventFactory.createIgnorableSpace(new String(ch, start, length));
        postEvent(event);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (destroyed) return;

        XMLEvent event = eventFactory.createProcessingInstruction(target, data);
        postEvent(event);
    }

    public void skippedEntity(String name) throws SAXException {
        // don't care
    }

    private String getPrefix(String qname) {
        if (qname == null) return null;

        int index = qname.indexOf(':');
        if (index < 1) {
            return "";
        }
        return qname.substring(0, index);
    }

    private class SaxLocation implements Location {
        public int getLineNumber() {
            return locator.getLineNumber();
        }

        public int getColumnNumber() {
            return locator.getColumnNumber();
        }

        public int getCharacterOffset() {
            return -1;
        }

        public String getPublicId() {
            return locator.getPublicId();
        }

        public String getSystemId() {
            return locator.getSystemId();
        }
    }

    private static class StaxParserWorker implements Runnable {
        private final StaxParser staxParser;
        private final long timeout;
        private final SynchronousQueue<XMLEvent> queue;
        private AtomicBoolean shouldStopNotifier = new AtomicBoolean(false);
        private boolean sawEOF;
        private boolean closed;

        private StaxParserWorker(StaxParser staxParser, long timeout, SynchronousQueue<XMLEvent> queue, AtomicBoolean shouldStopNotifier) {
            this.staxParser = staxParser;
            this.timeout = timeout;
            this.queue = queue;
            this.shouldStopNotifier = shouldStopNotifier;
        }

        public void run() {
            try {
                staxParser.parse(new QueueXMLEventReader());
            } finally {

                // tell the sax pusher that we are done and it should stop sending events
                shouldStopNotifier.set(true);

                // wait for the pusher to send us the eof
                while (!sawEOF) {
                    try {
                        XMLEvent event = queue.take();
                        if (event == EOF) {
                            sawEOF = true;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }

        public class QueueXMLEventReader implements XMLEventReader {
            private XMLEvent current;
            private XMLEvent next;

            private XMLEvent getNext() {
                if (closed) {
                    return null;
                }

                if (next != null) {
                    return next;
                }

                try {
                    next = queue.poll(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }

                if (next == null) {
                    return null;
                }

                if (next == EOF) {
                    sawEOF = true;
                    return null;
                }

                return next;
            }

            public Object next() {
                return nextEvent();
            }

            public XMLEvent nextEvent() throws NoSuchElementException {
                if (closed) throw new NoSuchElementException("Stream is closed");

                current = getNext();
                next = null;
                if (current == null) {
                    throw new NoSuchElementException();
                }
                return current;
            }

            public boolean hasNext() {
                return getNext() != null;
            }

            public XMLEvent peek() throws XMLStreamException {
                if (closed) throw new XMLStreamException("Stream is closed");

                return getNext();
            }

            public String getElementText() throws XMLStreamException {
                if (closed) throw new XMLStreamException("Stream is closed");

                if (!(current instanceof StartElement)) {
                    throw new XMLStreamException("Current event must be StartElement, but is " + current.getClass().getSimpleName());
                }
                StringBuilder builder = new StringBuilder();
                while (true){
                    XMLEvent event = nextEvent();
                    if (event instanceof EndElement) {
                        return builder.toString();
                    } else if (event instanceof Characters) {
                        Characters characters = (Characters) event;
                        builder.append(characters.getData());
                    } else if (event instanceof Comment) {
                        // ignored
                    } else if (event instanceof DTD) {
                        // ignored
                    } else if (event instanceof ProcessingInstruction) {
                        // ignored
                    } else if (event instanceof Namespace) {
                        // ignored
                    } else if (event instanceof Attribute) {
                        // ignored
                    } else {
                        throw new XMLStreamException("Encountered XMLEvent event " + event.getClass().getSimpleName());
                    }
                }
            }

            public XMLEvent nextTag() throws XMLStreamException {
                if (closed) throw new XMLStreamException("Stream is closed");

                while (true) {
                    XMLEvent event = nextEvent();
                    if (event instanceof StartElement || event instanceof EndElement) {
                        return event;
                    } else if (event instanceof Characters) {
                        Characters characters = (Characters) event;
                        if (!characters.isIgnorableWhiteSpace()) {
                            throw new XMLStreamException("Encountered non-ignorable whitespace \"" + characters.getData() + "\"");
                        }
                    } else if (event instanceof Comment) {
                        // ignored
                    } else if (event instanceof DTD) {
                        // ignored
                    } else if (event instanceof ProcessingInstruction) {
                        // ignored
                    } else if (event instanceof Namespace) {
                        // ignored
                    } else if (event instanceof Attribute) {
                        // ignored
                    } else {
                        throw new XMLStreamException("Encountered XMLEvent event " + event.getClass().getSimpleName());
                    }
                }
            }

            public Object getProperty(String name) throws IllegalArgumentException {
                return null;
            }

            public void close() {
                closed = true;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class EOF implements XMLEvent {
        public int getEventType() {
            throw new UnsupportedOperationException("EOF");
        }

        public Location getLocation() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isStartElement() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isAttribute() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isNamespace() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isEndElement() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isEntityReference() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isProcessingInstruction() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isCharacters() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isStartDocument() {
            throw new UnsupportedOperationException("EOF");
        }

        public boolean isEndDocument() {
            throw new UnsupportedOperationException("EOF");
        }

        public StartElement asStartElement() {
            throw new UnsupportedOperationException("EOF");
        }

        public EndElement asEndElement() {
            throw new UnsupportedOperationException("EOF");
        }

        public Characters asCharacters() {
            throw new UnsupportedOperationException("EOF");
        }

        public QName getSchemaType() {
            throw new UnsupportedOperationException("EOF");
        }

        public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
            throw new UnsupportedOperationException("EOF");
        }
    }
}
