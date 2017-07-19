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
 **$Log: not supported by cvs2svn $
 **Revision 1.22  2007/11/16 17:03:00  jfdenise
 **Added some checks and handle stop then start of the Timer.
 **
 **Revision 1.21  2007/11/16 15:12:13  jfdenise
 **Fix for bug 147 and 148
 **
 **Revision 1.20  2007/11/07 11:15:35  denis_rachal
 **Issue number:  142 & 146
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **
 **142: EventingSupport.retrieveContext(UUID) throws RuntimeException
 **
 **Fixed WSEventingSupport to not throw RuntimeException. Instead it throws a new InvalidSubscriptionException. EventingSupport methods still throw RuntimeException to maintain backward compatibility.
 **
 **146: Enhance to allow specifying default expiration per enumeration
 **
 **Also enhanced WSEventingSupport to allow setting the default expiration per subscription. Default if not set by developer or client is now 24 hours for subscriptions.
 **
 **Additionally added javadoc to both EventingSupport and WSEventingSupport.
 **
 **Revision 1.19  2007/09/18 13:06:56  denis_rachal
 **Issue number:  129, 130 & 132
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **
 **129  ENHANC  P2  All  denis_rachal  NEW   Need support for ReNew Operation in Eventing
 **130  DEFECT  P3  x86  jfdenise  NEW   Should return a boolean variable result not a constant true
 **132  ENHANC  P3  All  denis_rachal  NEW   Make ServletRequest attributes available as properties in Ha
 **
 **Added enhancements and fixed issue # 130.
 **
 **Revision 1.18  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: BaseSupport.java,v 1.23 2008-01-17 15:19:09 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.util.Date;
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
    static class ExpirationTask extends TimerTask {
        private UUID context;
        ExpirationTask(UUID context) {
            this.context = context;
        }
        
        public void run() {
            BaseContext ctx = contextMap.get(context);
            
            if(ctx == null || ctx.isDeleted()) return;
            
            final GregorianCalendar now = new GregorianCalendar();
            final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);
            if(ctx.isExpired(nowXml)) {
                ctx.setDeleted();
                if(ctx.getListener() != null)
                    ctx.getListener().contextUnbound(null, context);
                contextMap.remove(context);
            } else {
                // Re-do tracking
                schedule(context, null, ctx);
            }
        }
    }
    protected static final String UUID_SCHEME = "urn:uuid:";
    protected static final String UNINITIALIZED = "uninitialized";
    protected static DatatypeFactory datatypeFactory = null;
    private static Duration defaultExpiration = null;
    private static Duration infiniteExpiration = null;

    public static final Map<UUID, BaseContext> contextMap = new ConcurrentHashMap<UUID, BaseContext>();

    private static final Logger LOG = Logger.getLogger(BaseSupport.class.getName());

    private static Timer cleanupTimer;

    private static Map<String, FilterFactory> supportedFilters =
            new HashMap<String, FilterFactory>();

    protected BaseSupport() {}

    static {
        FilterFactory xpathFilter = new XPathFilterFactory();
        supportedFilters.put(com.sun.ws.management.xml.XPath.NS_URI,
                xpathFilter);
        try {
            datatypeFactory = DatatypeFactory.newInstance();
            defaultExpiration = datatypeFactory.newDuration(true, 0, 0, 0, 0, 10, 0); // 10 minutes
            infiniteExpiration = datatypeFactory.newDuration(true, 1, 0, 0, 0, 0, 0); // 1 year
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
 
        final Object expiresObj = unmarshalExpires(expires);
        final XMLGregorianCalendar now = 
        	datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar());

        final XMLGregorianCalendar expiration;
        if (expiresObj instanceof Duration) {
            expiration = (XMLGregorianCalendar)now.clone();
            expiration.add((Duration)expiresObj);
        } else {
        	expiration = (XMLGregorianCalendar)expiresObj;
        }
		// Check if it is in the past
        if (now.compare(expiration) > 0)
            throw new InvalidExpirationTimeFault();
        return expiration;
    }
    
    protected static Object unmarshalExpires(String expires)
    	throws InvalidExpirationTimeFault {
        try {
            // first try if it's a Duration
            final Duration duration = datatypeFactory.newDuration(expires);
            return duration;
        } catch (IllegalArgumentException e) {
            try {
                // now see if it is a calendar time
            	final XMLGregorianCalendar calendar =
            		datatypeFactory.newXMLGregorianCalendar(expires);
            	return calendar;
            } catch (IllegalArgumentException ncex) {
                throw new InvalidExpirationTimeFault();
            }
        }
    }
    
    protected static String computeExpiration(final String expires,
			                                  Duration defExpiration,
			                                  Duration maxExpiration) 
    	throws InvalidExpirationTimeFault {
    	// Check default & maximum
    	if (defExpiration == null)
    		defExpiration = defaultExpiration;
    	if (maxExpiration == null)
    		maxExpiration = infiniteExpiration;
    	
    	// Check if nothing is to be returned to the client
    	if ((expires == null) 
    			&& (!(defExpiration.compare(infiniteExpiration) == DatatypeConstants.LESSER))
    			&& (!(maxExpiration.compare(infiniteExpiration) == DatatypeConstants.LESSER)))
    		return null;
    	
    	if ((expires == null) || (expires.length() == 0)) {
    		// Compute the default expiration for the caller
    		final GregorianCalendar now = new GregorianCalendar();
    		final XMLGregorianCalendar expiration = datatypeFactory.newXMLGregorianCalendar(now);
            expiration.add(defExpiration);
            return expiration.toString();
    	}
    	
    	// Caller specified a value. Find out its type.
    	final Object callerExpiration = unmarshalExpires(expires);
    	if (callerExpiration instanceof XMLGregorianCalendar) {
            // Compute the expiration for the caller
    		final XMLGregorianCalendar expiration = (XMLGregorianCalendar) callerExpiration;
    		final XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar());
    		
    		// Check if it is in the past
            if (now.compare(expiration) > 0)
                throw new InvalidExpirationTimeFault();

            // Check if it exceeds the maximum allowed
        	final XMLGregorianCalendar max = (XMLGregorianCalendar)now.clone();
        	max.add(maxExpiration);
        	if (expiration.compare(max) == DatatypeConstants.GREATER)
        		return max.toString();
        	else
        		return expiration.toString();
    	} else {
			// Caller specified a Duration
			final Duration request = (Duration) callerExpiration;
			
			// Check if it exceeds the maximum allowed
			if (request.compare(maxExpiration) == DatatypeConstants.GREATER)
				return maxExpiration.toString();
			else
				return request.toString();
		}
	}
    
    private static synchronized Timer getTimer() {
        if(cleanupTimer == null)
            cleanupTimer = new Timer(true);
        return cleanupTimer;
    }
    
    private static void schedule(final UUID uuid, final HandlerContext requestContext,
            final BaseContext context) {
        try {
            Date date = context.getExpirationDate();
            if(date != null) {
                final ExpirationTask task = new ExpirationTask(uuid);
                getTimer().schedule(task, date);
            }
        } catch(Exception ex) {
           // ex.printStackTrace();
            context.getListener().contextUnbound(requestContext, uuid);
            contextMap.remove(uuid);
            throw new InvalidExpirationTimeFault();
        }
    }
    
    protected static UUID initContext(final HandlerContext requestContext,
            final BaseContext context) {
        final UUID uuid = UUID.randomUUID();
        contextMap.put(uuid, context);
        if (context.getListener() != null) {
            context.getListener().contextBound(requestContext, uuid);
        }
        schedule(uuid, requestContext, context);
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
    
    protected synchronized static BaseContext renewContext(final XMLGregorianCalendar expiration,
            final Object context) {
    	BaseContext ctx = contextMap.get(context);
    	if (ctx == null)
    		return null;
    	synchronized (ctx) {
    		if (ctx.isDeleted() == false) {
    			ctx.renew(expiration);
    			return ctx;
    		} else {
    			return null;
    		}
    	}
    }

    protected synchronized static BaseContext removeContext(final HandlerContext requestContext,
    		                                   final Object context) {
    	BaseContext ctx = contextMap.get(context);
    	if (ctx == null)
    		return null;
    	synchronized (ctx) {
			// Set to deleted in case another thread still has a reference to
			// this context.
			if (ctx.isDeleted() == false) {
				ctx.setDeleted();
				if (ctx.getListener() != null) {
					ctx.getListener().contextUnbound(requestContext,
							(UUID) context);
				}
				return contextMap.remove(context);
			} else {
				return null;
			}
		}
	}
}