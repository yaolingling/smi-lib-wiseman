/*
 * Copyright 2005 Sun Microsystems, Inc.
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
 **Revision 1.29  2007/11/07 11:15:35  denis_rachal
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
 **Revision 1.28  2007/10/31 12:25:17  jfdenise
 **Split between new support and previous one.
 **
 **Revision 1.27  2007/10/30 09:27:47  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.26  2007/10/02 10:43:44  jfdenise
 **Fix for bug ID 134, Enumeration Iterator look up is static
 **Applied to Enumeration and Eventing
 **
 **Revision 1.25  2007/09/18 13:06:56  denis_rachal
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
 **Revision 1.24  2007/06/13 13:19:02  jfdenise
 **Fix for BUG ID 115 : EventingSupport should be able to create an event msg
 **
 **Revision 1.23  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EventingSupport.java,v 1.31 2007-12-20 20:47:52 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathExpressionException;

import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.eventing.InvalidSubscriptionException;
import com.sun.ws.management.server.message.SAAJMessage;
import com.sun.ws.management.soap.FaultException;

/**
 * A helper class that encapsulates some of the arcane logic to manage
 * subscriptions using the WS-Eventing protocol.
 */
public final class EventingSupport extends WSEventingBaseSupport {
    
    private static Map<String, EventingIteratorFactory> registeredIterators =
            new HashMap<String, EventingIteratorFactory>();
    
    private EventingSupport() {}
    
    /**
     *  Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     * operation.
     * 
     * @param handlerContext
     *        The handler context for this request
     * @param request
     *        The incoming SOAP message that contains the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     *        request.
     * @param response
     *        The empty SOAP message that will contain the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse SubscribeResponse}.
     * @param listener
     *        Will be called when the subscription is successfully created and
     *        when deleted.
     *        
     * @return UUID that identifies the subscription created
     * 
     * @throws DatatypeConfigurationException
     * @throws SOAPException
     * @throws JAXBException
     * @throws FaultException
     */
    public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        return subscribe(handlerContext, request, response, listener, null);
    }
    
    /**
     *  Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     * operation.
     * 
     * @param handlerContext
     *        The handler context for this request
     * @param request
     *        The incoming SOAP message that contains the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     *        request.
     * @param response
     *        The empty SOAP message that will contain the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse SubscribeResponse}.
     * @param listener
     *        Will be called when the subscription is successfully created and
     *        when deleted.
     * @param factory
     *        The iterator factory to use to create the iterator
     *        for pull type subscriptions.
     *        If null and this is a pull type subscription, then the default
     *        iterator EventingIterator will be created.
     *        {@link #sendEvent(UUID, Object)}
     *        may be called to add events to this iterator.
     *        
     * @return UUID that identifies the subscription created
     * 
     * @throws DatatypeConfigurationException
     * @throws SOAPException
     * @throws JAXBException
     * @throws FaultException
     */
    public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final ContextListener listener, final EventingIteratorFactory factory)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        return subscribe(handlerContext, request, response, false, DEFAULT_QUEUE_SIZE, listener, factory);
    }
    
    /**
     *  Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     * operation.
     * 
     * @param handlerContext
     *        The handler context for this request
     * @param request
     *        The incoming SOAP message that contains the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
     *        request.
     * @param response
     *        The empty SOAP message that will contain the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse SubscribeResponse}.
     * @param isFiltered
     *        Indicates that events have been pre-filtered and that this module
     *        should not filter events.
     * @param queueSize
     *        If the request is a pull type subscription and the default iterator
     *        factory is used, this indicates how large to make the queue for this iterator.
     *        If {@link #sendEvent(UUID, Object)} is called and the queue size is exceeded
     *        the oldest entry is deleted and the new entry is added at the end of the queue.
     * @param listener
     *        Will be called when the subscription is successfully created and
     *        when deleted.
     * @param factory
     *        The iterator factory to use to create the iterator
     *        for pull type subscriptions.
     *        If null and this is a pull type subscription, then the default
     *        iterator EventingIterator will be created.
     *        {@link #sendEvent(UUID, Object)}
     *        may be called to add events to this iterator.
     *        
     * @return UUID that identifies the subscription created
     * 
     * @throws DatatypeConfigurationException
     * @throws SOAPException
     * @throws JAXBException
     * @throws FaultException
     */
     public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final boolean isFiltered,
            final int queueSize,
            final ContextListener listener, 
            EventingIteratorFactory factory)
            throws DatatypeConfigurationException, SOAPException, JAXBException, FaultException {
         SAAJMessage msg = new SAAJMessage(new Management(request));
         SAAJMessage resp = new SAAJMessage(new Management(response));
         return WSEventingSupport.subscribe(handlerContext, msg, resp, isFiltered, queueSize, listener, factory);
     }
    
     /**
      *  Initiate an
      * {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
      * operation.
      * 
      * @param handlerContext
      *        The handler context for this request
      * @param request
      *        The incoming SOAP message that contains the
      *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe Subscribe}
      *        request.
      * @param response
      *        The empty SOAP message that will contain the
      *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse SubscribeResponse}.
      * @param isFiltered
      *        Indicates that events have been pre-filtered and that this module
      *        should not filter events.
      * @param queueSize
      *        If the request is a pull type subscription and the default iterator
      *        factory is used, this indicates how large to make the queue for this iterator.
      *        If {@link #sendEvent(UUID, Object)} is called and the queue size is exceeded
      *        the oldest entry is deleted and the new entry is added at the end of the queue.
      * @param listener
      *        Will be called when the subscription is successfully created and
      *        when deleted.
      *        
      * @return UUID that identifies the subscription created
      * 
      * @throws DatatypeConfigurationException
      * @throws SOAPException
      * @throws JAXBException
      * @throws FaultException
      */
    public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final boolean isFiltered,
            final int queueSize,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException, JAXBException, FaultException {
         return subscribe(handlerContext, request, response,isFiltered,queueSize,listener, null);
    }
    
    /**
     * Create a subscription Manager EPR for the given subscription request.
     * 
     * @param request subscription request
     * @param response response object
     * @param context subscription context
     * 
     * @return EPR to the subscription manager of this subscription
     * 
     * @throws SOAPException
     * @throws JAXBException
     */
    public static EndpointReferenceType createSubscriptionManagerEpr(
            final Eventing request, final Eventing response,
            final Object context) throws SOAPException, JAXBException {
         SAAJMessage msg = new SAAJMessage(new Management(request));
         SAAJMessage resp = new SAAJMessage(new Management(response));
         return WSEventingSupport.createSubscriptionManagerEpr(msg, resp,context);
     }
     
    /**
     * Renews an existing subscription. The subscription must still be
     * active and not already canceled.
     * 
     * @param handlerContext
     *        The handler context for this request
     * @param request
     *      The incoming SOAP message that contains the
     *      {@link org.xmlsoap.schemas.ws._2004._08.eventing.Renew Renew}
     *      request.
     * @param response
     *        The empty SOAP message that will contain the
     *        {@link org.xmlsoap.schemas.ws._2004._08.eventing.RenewResponse RenewResponse}.
     *        
     * @throws SOAPException
     * @throws JAXBException
     * @throws FaultException if the subscription does not exist
     */
    public static void renew(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response)
            throws SOAPException, JAXBException, FaultException {
         SAAJMessage msg = new SAAJMessage(new Management(request));
         SAAJMessage resp = new SAAJMessage(new Management(response));
         WSEventingSupport.renew(handlerContext, msg, resp);
    }
    
    /**
     * Unsubscribe an existing subscription. This method cancels
     * an existing subscription.
     * 
     * @param handlerContext
     *        The handler context for this request
     * @param request
     *      The incoming SOAP message that contains the
     *      {@link org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe Unsubscribe}
     *      request.
     * @param response
     *        The empty SOAP message that will contain the unsubscribe response.
     *        
     * @throws SOAPException
     * @throws JAXBException
     * @throws FaultException if the subscription does not exist
     */
    public static void unsubscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response)
            throws SOAPException, JAXBException, FaultException {
          SAAJMessage msg = new SAAJMessage(new Management(request));
         SAAJMessage resp = new SAAJMessage(new Management(response));
         WSEventingSupport.unsubscribe(handlerContext, msg, resp);
    }
    
    /**
     * Send an event for a specified subscription id. If the subscription
     * is of type pull the event will be added to the pull iterator.
     * If the subscription is of type push the event will be sent
     * immidiately to the subscriber.
     * NOTE: For push this method currently blocks until the event
     *       has been successfully delivered to the client.
     * 
     * @param id UUID identifying the subscription this event is for
     * @param content the event to send to the subscriber
     * 
     * @return true if the event was successfully queued or delivered,
     *         otherwise false
     * 
     * @throws SOAPException
     * @throws JAXBException
     * @throws IOException
     */
    public static boolean sendEvent(UUID id, Object content)
    throws SOAPException, JAXBException, IOException {
        
    	try {
    		return WSEventingSupport.sendEvent(id, content);
    	} catch (InvalidSubscriptionException e) {
    		// for backwards compatibility
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Send an event for a specified subscription context.
     * 
     * @param context subscription context
     * @param msg message to use when sending  the request
     * 
     * @return true is the event was successfully sent, otherwise false
     * 
     * @throws SOAPException
     * @throws JAXBException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws Exception
     */
    public static boolean sendEvent(final Object context, final Addressing msg,
            final NamespaceMap nsMap)
            throws SOAPException, JAXBException, IOException, XPathExpressionException, Exception {
        
    	try {
    		if ((context instanceof UUID) == false) {
    			throw new RuntimeException("Subscription context is not valid");
    		}
    		return WSEventingSupport.sendEvent((UUID)context, msg);
    	} catch (InvalidSubscriptionException e) {
    		// for backwards compatibility
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Create a Filter from an Eventing request
     *
     * @return Returns a Filter object if a filter exists in the request, otherwise null.
     * @throws CannotProcessFilterFault, FilteringRequestedUnavailableFault, InternalErrorFault
     */
    public static Filter createFilter(final Eventing request)
    throws CannotProcessFilterFault, FilteringRequestedUnavailableFault {
        SAAJMessage msg;
        try {
            msg = new SAAJMessage(new Management(request));
        } catch (SOAPException ex) {
            throw new InternalErrorFault(ex.getMessage());
        }
        return WSEventingSupport.createFilter(msg);
    }
    
    public static NamespaceMap getNamespaceMap(final Eventing request) {
        SAAJMessage msg;
        try {
            msg = new SAAJMessage(new Management(request));
        } catch (SOAPException ex) {
            throw new InternalErrorFault(ex.getMessage());
        }
         return WSEventingSupport.getNamespaceMap(msg);
    }
    
    /**
     * Add an iterator factory to EnumerationSupport.
     *
     * @param resourceURI ResourceURI for which this iterator factory
     * is to be used to fufill Enumeration requests.
     * @param iteratorFactory The Iterator Factory that creates <code>EnumerationIterator</code>
     * objects that are used by EnumerationSupport to fufill Enumeration requests.
     * If a factory is already registered it will be overwritten with the specified
     * factory.
     */
    public synchronized static void registerIteratorFactory(String resourceURI,
            EventingIteratorFactory iteratorFactory) throws Exception {
        registeredIterators.put(resourceURI, iteratorFactory);
    }
    
    /**
     * Gets an IteratorFactory for the specified resource URI.
     *
     * @param resourceURI the URI associated with the IteratorFactory
     * @return the IteratorFactory if one is registered, otherwise null
     */
    public synchronized static EventingIteratorFactory getIteratorFactory(String resourceURI) {
        return registeredIterators.get(resourceURI);
    }
}
