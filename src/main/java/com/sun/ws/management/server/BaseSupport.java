/*
 * Copyright 2006 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: BaseSupport.java,v $
 **Revision 1.18  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: BaseSupport.java,v 1.18 2007/05/30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.sun.ws.management.Management;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.enumeration.InvalidExpirationTimeFault;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.xml.XPathFilterFactory;

public class BaseSupport {

    protected static final String UUID_SCHEME = "urn:uuid:";
    protected static final String UNINITIALIZED = "uninitialized";
    protected static DatatypeFactory datatypeFactory = null;

    public static final Map<UUID, BaseContext> contextMap = new ConcurrentHashMap<UUID, BaseContext>();

    protected static final TimerTask ttask = new TimerTask() {
        public void run() {
            final GregorianCalendar now = new GregorianCalendar();
            final XMLGregorianCalendar nowXml =
                    datatypeFactory.newXMLGregorianCalendar(now);
            final UUID[] keys = contextMap.keySet().toArray(new UUID[contextMap.size()]);
            for (int i = 0; i < keys.length; i++) {
                final UUID key = keys[i];
                final BaseContext context = contextMap.get(key);
                if (context != null) {
					if (context.isExpired(nowXml)) {
						removeContext(null, key);
					}
				}
            }
        }
    };

    private static final Logger LOG = Logger.getLogger(BaseSupport.class.getName());

    private static final int CLEANUP_INTERVAL = 60000;
    private static Timer cleanupTimer = new Timer(true);

    private static Map<String, FilterFactory> supportedFilters =
            new HashMap<String, FilterFactory>();

    protected BaseSupport() {}

    static {
        FilterFactory xpathFilter = new XPathFilterFactory();
        supportedFilters.put(com.sun.ws.management.xml.XPath.NS_URI,
                xpathFilter);
        try {
            datatypeFactory = DatatypeFactory.newInstance();
            // try{
            cleanupTimer.schedule(ttask, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
        /*} catch(java.lang.IllegalStateException e){
            // NOTE: the cleanup timer has been throwing this
            // exception during unit tests. Re-initalizing it should not
            // cause it to fail so this exception is being silenced.
            LOG.fine("Base support was re-initalized.");
        }*/
        } catch(Exception ex) {
            throw new RuntimeException("Fail to initialize BaseSupport " + ex);
        }
    }
    /**
     * Add a Filtering support for a specific dialect.
     * @param dialect Filter dialect
     * @param filterFactory The Filter Factory that creates <code>Filter</code> for requests
     * relying on the passed dialect.
     *
     * @throws java.lang.Exception If the filter is already supported.
     */
    public synchronized static void addSupportedFilterDialect(String dialect,
            FilterFactory filterFactory) throws Exception {
        if(supportedFilters.get(dialect) != null)
            throw new Exception("Dialect " + dialect + " already supported");
        supportedFilters.put(dialect, filterFactory);
    }

    /**
     * Determines if the passed dialect is a supported dialect
     * @param dialect The dialect to check for support.
     * @return true if it is a supported dialect (or if dialect is null == default), else false
     */
    public synchronized static boolean isSupportedDialect(final String dialect) {
        if(dialect == null) return true;
        return supportedFilters.get(dialect) != null;
    }

    /**
     * Supported dialects, returned as Fault Detail when the dialect
     * is not supported.
     * @return An array of supported dialects.
     */
    public synchronized static String[] getSupportedDialects() {
        Set<String> keys =  supportedFilters.keySet();
        String[] dialects = new String[keys.size()];
        return keys.toArray(dialects);
    }

    protected synchronized static Filter newFilter(String dialect,
            List content,
            NamespaceMap nsMap) throws Exception {
        if(dialect == null)
            dialect = com.sun.ws.management.xml.XPath.NS_URI;
        FilterFactory factory = supportedFilters.get(dialect);
        if(factory == null)
            throw new FilteringRequestedUnavailableFault(null,
                    getSupportedDialects());
        return factory.newFilter(content, nsMap);
    }

    public static Filter createFilter(String dialect, List content,
            NamespaceMap nsMap)throws CannotProcessFilterFault,
            FilteringRequestedUnavailableFault {
        try {
            return newFilter(dialect, content, nsMap);
        }catch(FaultException fex) {
            throw fex;
        } catch(Exception ex) {
            throw new CannotProcessFilterFault(ex.getMessage());
        }
    }


    /**
     * Create a JAXBElement object that is a wsman:XmlFragment from a list
     * of XML Nodes.
     *
     * @param nodes Nodes to be inserted into the XmlFragment element.
     * @return XmlFragment JAXBElement object.
     */
	public static JAXBElement<MixedDataType> createXmlFragment(List<Node> nodes) {
		final MixedDataType mixedDataType = Management.FACTORY.createMixedDataType();
		final StringBuffer buf = new StringBuffer();

		for (int j = 0; j < nodes.size(); j++) {
			// Check if it is a text node from text() function
			if (nodes.get(j) instanceof Element) {
				if (buf.length() > 0) {
					mixedDataType.getContent().add(buf.toString());
					buf.setLength(0);
				}
				mixedDataType.getContent().add(nodes.get(j));
			} else if (nodes.get(j) instanceof Text) {
				buf.append(nodes.get(j).getTextContent());
			}
		}
		if (buf.length() > 0) {
			mixedDataType.getContent().add(buf.toString());
			buf.setLength(0);
		}
		final JAXBElement<MixedDataType> fragment = Management.FACTORY
				.createXmlFragment(mixedDataType);
        //if there was no content, then this is NIL
        if (nodes.size() <= 0) {
        	fragment.setNil(true);
        }
		return fragment;
	}

    /**
     * Create a JAXBElement object that is a wsman:XmlFragment from a NodeList
     *
     * @param nodes Nodes to be inserted into the XmlFragment element.
     * @return XmlFragment JAXBElement object.
     */
	public static JAXBElement<MixedDataType> createXmlFragment(NodeList nodes) {
		final MixedDataType mixedDataType = Management.FACTORY.createMixedDataType();
		final StringBuffer buf = new StringBuffer();

		for (int j = 0; j < nodes.getLength(); j++) {
			// Check if it is a text node from text() function
			if (nodes.item(j) instanceof Element) {
				if (buf.length() > 0) {
					mixedDataType.getContent().add(buf.toString());
					buf.setLength(0);
				}
				mixedDataType.getContent().add(nodes.item(j));
			} else if (nodes.item(j) instanceof Text) {
				buf.append(nodes.item(j).getTextContent());
			}
		}
		if (buf.length() > 0) {
			mixedDataType.getContent().add(buf.toString());
			buf.setLength(0);
		}
		final JAXBElement<MixedDataType> fragment = Management.FACTORY
				.createXmlFragment(mixedDataType);
        //if there was no content, then this is NIL
        if (nodes.getLength() <= 0) {
        	fragment.setNil(true);
        }
		return fragment;
	}

	/* The following code was a test as a possible replacement for createXmlFragment()
	public static Element createXmlFragmentElement(NodeList nodes) {
		final Document doc = Message.newDocument();
		final Element fragment = doc.createElementNS(Management.NS_URI, "XmlFragment");
		doc.appendChild(fragment);
		for (int j = 0; j < nodes.getLength(); j++) {
			fragment.appendChild(doc.adoptNode(nodes.item(j)));
		}
		return fragment;
	} */

    protected static XMLGregorianCalendar initExpiration(final String expires)
    throws InvalidExpirationTimeFault {

        assert datatypeFactory != null : UNINITIALIZED;

        if (expires == null) {
            // a very large value - effectively never expires
            return datatypeFactory.newXMLGregorianCalendar(Integer.MAX_VALUE,
                    12, 31, 23, 59, 59, 999, DatatypeConstants.MAX_TIMEZONE_OFFSET);
        }

        final GregorianCalendar now = new GregorianCalendar();
        final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);

        XMLGregorianCalendar expiration = null;
        try {
            // first try if it's a Duration
            final Duration duration = datatypeFactory.newDuration(expires);
            expiration = datatypeFactory.newXMLGregorianCalendar(now);
            expiration.add(duration);
        } catch (IllegalArgumentException ndex) {
            try {
                // now see if it is a calendar time
                expiration = datatypeFactory.newXMLGregorianCalendar(expires);
            } catch (IllegalArgumentException ncex) {
                throw new InvalidExpirationTimeFault();
            }
        }
        if (nowXml.compare(expiration) > 0) {
            // expiration cannot be in the past
            throw new InvalidExpirationTimeFault();
        }
        return expiration;
    }

    protected static UUID initContext(final HandlerContext requestContext,
    		                          final BaseContext context) {
        final UUID uuid = UUID.randomUUID();
        contextMap.put(uuid, context);
		if (context.getListener() != null) {
			context.getListener().contextBound(requestContext, uuid);
		}
        return uuid;
    }

    protected static BaseContext getContext(final Object context) {
        return contextMap.get(context);
    }

    protected static BaseContext putContext(final UUID context, final BaseContext ctx) {
        return contextMap.put(context, ctx);
    }

    public static synchronized void stopTimer() {
        if(LOG.isLoggable(Level.FINER))
            LOG.log(Level.FINER, "Stopping timer " + cleanupTimer);
        if(cleanupTimer == null) return;
        cleanupTimer.cancel();
        cleanupTimer = null;
    }

    protected synchronized static BaseContext removeContext(final HandlerContext requestContext,
    		                                   final Object context) {
    	BaseContext ctx = contextMap.get(context);
    	if (ctx == null)
    		return null;
    	// Set to deleted in case another thread still has a reference to this context.
    	ctx.setDeleted();
    	if (ctx.getListener() != null) {
    		ctx.getListener().contextUnbound(requestContext, (UUID)context);
    	}
        return contextMap.remove(context);
    }
}