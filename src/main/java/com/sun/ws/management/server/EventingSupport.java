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
 **$Log: EventingSupport.java,v $
 **Revision 1.24  2007/06/13 13:19:02  jfdenise
 **Fix for BUG ID 115 : EventingSupport should be able to create an event msg
 **
 **Revision 1.23  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EventingSupport.java,v 1.24 2007/06/13 13:19:02 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferencePropertiesType;
import org.xmlsoap.schemas.ws._2004._08.eventing.DeliveryType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.eventing.DeliveryModeRequestedUnavailableFault;
import com.sun.ws.management.eventing.EventSourceUnableToProcessFault;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.eventing.InvalidMessageFault;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transport.HttpClient;

/**
 * A helper class that encapsulates some of the arcane logic to manage
 * subscriptions using the WS-Eventing protocol.
 */
public final class EventingSupport extends BaseSupport {
    
    public static final int DEFAULT_QUEUE_SIZE = 1024;
    public static final int DEFAULT_EXPIRATION_MILLIS = 60000;
    
    // TODO: add more delivery modes as they are implemented
    private static final String[] SUPPORTED_DELIVERY_MODES = {
        Eventing.PUSH_DELIVERY_MODE,
        EventingExtensions.PULL_DELIVERY_MODE
    };
    
    private static Map<String, EventingIteratorFactory> registeredIterators =
            new HashMap<String, EventingIteratorFactory>();
    
    private static Duration defaultExpiration = null;
    
    static {
        defaultExpiration = datatypeFactory.newDuration(DEFAULT_EXPIRATION_MILLIS);
    }
    
    private EventingSupport() {}
    
    public static String[] getSupportedDeliveryModes() {
        return SUPPORTED_DELIVERY_MODES;
    }
    
    public static boolean isDeliveryModeSupported(final String deliveryMode) {
        for (final String mode : SUPPORTED_DELIVERY_MODES) {
            if (mode.equals(deliveryMode)) {
                return true;
            }
        }
        return false;
    }
    
    // the EventingExtensions.PULL_DELIVERY_MODE is handled by
    // EnumerationSupport
    public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        return subscribe(handlerContext, request, response, false, DEFAULT_QUEUE_SIZE, listener);
    }
    
    // the EventingExtensions.PULL_DELIVERY_MODE is handled by
    // EnumerationSupport
    public static UUID subscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response,
            final boolean isFiltered,
            final int queueSize,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException, JAXBException, FaultException {
        
        final Subscribe subscribe = request.getSubscribe();
        if (subscribe == null) {
            throw new InvalidMessageFault();
        }
        
        final EndpointReferenceType endTo = subscribe.getEndTo();
        if (endTo != null) {
            throw new UnsupportedFeatureFault(UnsupportedFeatureFault.Detail.ADDRESSING_MODE);
        }
        
        final DeliveryType delivery = subscribe.getDelivery();
        String deliveryMode = delivery.getMode();
        if (deliveryMode == null) {
            // implied value
            deliveryMode = Eventing.PUSH_DELIVERY_MODE;
        }
        
        Filter filter = null;
        
        if (deliveryMode.equals(EventingExtensions.PULL_DELIVERY_MODE)) {
            // this is a pull event mode subscribe request so setup an enumeration
            final EventingExtensions evtxRequest = new EventingExtensions(
                    request);
            
            EnumerationIterator iterator = newIterator(handlerContext,
                    request,
                    response,
                    isFiltered,
                    queueSize);
            
            
            
            if (iterator.isFiltered() == false) {
                // We will do the filtering
                try {
                    filter = createFilter(request);
                } catch (FilteringRequestedUnavailableFault fex) {
                    throw fex;
                } catch (Exception ex) {
                    throw new EventSourceUnableToProcessFault(ex.getMessage());
                }
            }
            
            if (subscribe.getEndTo() != null) {
                throw new UnsupportedFeatureFault(
                        UnsupportedFeatureFault.Detail.ADDRESSING_MODE);
            }
            
            // create and register an EnumerationContext
            EnumerationContext ctx = new EnumerationContext(initExpiration(subscribe.getExpires()),
                    filter, null, iterator, listener);
            
            // Set single thread use of this context
            synchronized (ctx) {
                final UUID context = initContext(handlerContext, ctx);
                // this is a pull event mode subscribe request
                final EventingExtensions evtx = new EventingExtensions(
                        response);
                evtx.setSubscribeResponse(EventingSupport
                        .createSubscriptionManagerEpr(request, response,
                        context), ctx.getExpiration(), context
                        .toString());
                return context;
            }
            
        } else {
            // one of the push modes
            if (isFiltered == false) {
                // We will do the filtering
                try {
                    filter = createFilter(request);
                } catch (FilteringRequestedUnavailableFault fex) {
                    throw fex;
                } catch (Exception ex) {
                    throw new EventSourceUnableToProcessFault(ex.getMessage());
                }
            }
            if (!isDeliveryModeSupported(deliveryMode)) {
                throw new DeliveryModeRequestedUnavailableFault(
                        SUPPORTED_DELIVERY_MODES);
            }
            
            EndpointReferenceType notifyTo = null;
            for (final Object content : delivery.getContent()) {
                final Class contentClass = content.getClass();
                if (JAXBElement.class.equals(contentClass)) {
                    final JAXBElement element = (JAXBElement) content;
                    final QName name = element.getName();
                    final Object item = element.getValue();
                    if (item instanceof EndpointReferenceType) {
                        final EndpointReferenceType epr = (EndpointReferenceType) item;
                        if (Eventing.NOTIFY_TO.equals(name)) {
                            notifyTo = epr;
                        }
                    }
                }
            }
            if (notifyTo == null) {
                throw new InvalidMessageFault(
                        "Event destination not specified: missing NotifyTo element");
            }
            if (notifyTo.getAddress() == null) {
                throw new InvalidMessageFault(
                        "Event destination not specified: missing NotifyTo.Address element");
            }
            
            EventingContext ctx = new EventingContext(initExpiration(subscribe
                    .getExpires()), filter, notifyTo, listener);
            
            final UUID context = initContext(handlerContext, ctx);
            response.setSubscribeResponse(createSubscriptionManagerEpr(request,
                    response, context), ctx.getExpiration());
            return context;
        }
    }
    
    public static EndpointReferenceType createSubscriptionManagerEpr(
            final Eventing request, final Eventing response,
            final Object context) throws SOAPException, JAXBException {
        
        final ReferenceParametersType refp = Addressing.FACTORY.createReferenceParametersType();
        final AttributableURI attributableURI = Management.FACTORY.createAttributableURI();
        Management mgmt = new Management(request);
        attributableURI.setValue(mgmt.getResourceURI());
        refp.getAny().add(Management.FACTORY.createResourceURI(attributableURI));
        
        final Document doc = response.newDocument();
        final Element identifier = doc.createElementNS(Eventing.IDENTIFIER.getNamespaceURI(),
                Eventing.IDENTIFIER.getPrefix() + ":" + Eventing.IDENTIFIER.getLocalPart());
        identifier.setTextContent(context.toString());
        doc.appendChild(identifier);
        refp.getAny().add(doc.getDocumentElement());
        return Addressing.createEndpointReference(request.getTo(), null, refp, null, null);
    }
    
    public static void unsubscribe(final HandlerContext handlerContext,
            final Eventing request,
            final Eventing response)
            throws SOAPException, JAXBException, FaultException {
        
        final Unsubscribe unsubscribe = request.getUnsubscribe();
        if (unsubscribe == null) {
            throw new InvalidMessageFault("Missing Unsubsribe element");
        }
        
        final String identifier = request.getIdentifier();
        if (identifier == null) {
            throw new InvalidMessageFault("Missing Identifier header element");
            
        }
        
        final Object found = removeContext(handlerContext,
                UUID.fromString(identifier));
        if (found == null) {
            /*
             * TODO: Convert to InvalidContextFault when available in
             * updated WS-Management specification
             */
            throw new InvalidMessageFault("Subscription with Identifier: " +
                    identifier + " not found");
        }
        
        response.setIdentifier(identifier);
    }
    
    // TODO: avoid blocking the sender - use a thread pool to send notifications
    public static boolean sendEvent(final Object context, final Addressing msg,
            final NamespaceMap nsMap)
            throws SOAPException, JAXBException, IOException, XPathExpressionException, Exception {
        
        assert datatypeFactory != null : UNINITIALIZED;
        
        final BaseContext bctx = getContext(context);
        if (bctx == null) {
            throw new RuntimeException("Context not found: subscription expired?");
        }
        if (!(bctx instanceof EventingContext)) {
            throw new RuntimeException("Context not found");
        }
        final EventingContext ctx = (EventingContext) bctx;
        
        final GregorianCalendar now = new GregorianCalendar();
        final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);
        if (ctx.isExpired(nowXml)) {
            removeContext(null, context);
            throw new RuntimeException("Subscription expired");
        }
        
        // the filter is only applied to the first child in soap body
        if (ctx.getFilter() != null) {
            final Node content = msg.getBody().getFirstChild();
            try {
                if (ctx.evaluate(content) == null)
                    return false;
            } catch (XPathExpressionException ex) {
                throw ex;
            }
        }
        
        final EndpointReferenceType notifyTo = ctx.getNotifyTo();
        msg.setTo(notifyTo.getAddress().getValue());
        final ReferenceParametersType refparams = notifyTo.getReferenceParameters();
        if (refparams != null) {
            msg.addHeaders(refparams);
        }
        final ReferencePropertiesType refprops = notifyTo.getReferenceProperties();
        if (refprops != null) {
            msg.addHeaders(refprops);
        }
        msg.setMessageId(UUID_SCHEME + UUID.randomUUID().toString());
        HttpClient.sendResponse(msg);
        return true;
    }
    
    /**
     * Create a Filter from an Eventing request
     *
     * @return Returns a Filter object if a filter exists in the request, otherwise null.
     * @throws CannotProcessFilterFault, FilteringRequestedUnavailableFault, InternalErrorFault
     */
    public static Filter createFilter(final Eventing request)
    throws CannotProcessFilterFault, FilteringRequestedUnavailableFault {
        try {
            final EventingExtensions evtxRequest = new EventingExtensions(request);
            final Subscribe subscribe = evtxRequest.getSubscribe();
            final org.xmlsoap.schemas.ws._2004._08.eventing.FilterType evtFilter = subscribe.getFilter();
            final DialectableMixedDataType evtxFilter = evtxRequest.getWsmanFilter();
            
            if ((evtFilter == null) && (evtxFilter == null)) {
                return null;
            }
            if ((evtFilter != null) && (evtxFilter != null)) {
                // Both are not allowed. Throw an exception
                throw new CannotProcessFilterFault(
                        SOAP.createFaultDetail(
                        "Both wse:Filter and wsman:Filter were specified in the request. Only one is allowed.",
                        null, null, null));
            }
            
            final NamespaceMap nsMap = getNamespaceMap(request);
            
            if (evtxFilter != null)
                return createFilter(evtxFilter.getDialect(),
                        evtxFilter.getContent(), nsMap);
            else
                return createFilter(evtFilter.getDialect(),
                        evtFilter.getContent(), nsMap);
        } catch (SOAPException e) {
            throw new InternalErrorFault(e.getMessage());
        } catch (JAXBException e) {
            throw new InternalErrorFault(e.getMessage());
        }
    }
    
    private static NamespaceMap getNamespaceMap(final Eventing request) {
        final NamespaceMap nsMap;
        final SOAPBody body = request.getBody();
        
        NodeList wsmanFilter = body.getElementsByTagNameNS(EventingExtensions.FILTER.getNamespaceURI(),
                EventingExtensions.FILTER.getLocalPart());
        NodeList evtFilter = body.getElementsByTagNameNS(Eventing.FILTER.getNamespaceURI(),
                Eventing.FILTER.getLocalPart());
        if ((wsmanFilter != null) && (wsmanFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(wsmanFilter.item(0));
        } else if ((evtFilter != null) && (evtFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(evtFilter.item(0));
        } else {
            NodeList evtElement = body.getElementsByTagNameNS(Eventing.SUBSCRIBE.getNamespaceURI(),
                    Eventing.SUBSCRIBE.getLocalPart());
            nsMap = new NamespaceMap(evtElement.item(0));
        }
        return nsMap;
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
    
    private synchronized static EnumerationIterator newIterator(
            final HandlerContext context,
            final Eventing request,
            final Eventing response,
            final boolean isFiltered,
            final int queueSize) throws SOAPException, JAXBException {
        final Management mgmt = new Management(request);
        final EventingIteratorFactory factory = registeredIterators.get(mgmt.getResourceURI());
        final DocumentBuilder db = response.getDocumentBuilder();
        
        if (factory == null) {
            // Build a default iterator for pull
            return new EventingIterator(isFiltered, queueSize);
        } else {
            // Build a custom iterator for pull
            return factory.newIterator(context, request, db, true, false);
        }
    }
    
    private static BaseContext retrieveContext(UUID id) {
        assert datatypeFactory != null : UNINITIALIZED;
        
        boolean result = false;
        BaseContext bctx = contextMap.get(id);
        if ((bctx == null) || (bctx.isDeleted())) {
            throw new RuntimeException("Context not found: subscription expired?");
        }
        
        // Check if context is expired
        final GregorianCalendar now = new GregorianCalendar();
        final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);
        if (bctx.isExpired(nowXml)) {
            removeContext(null, bctx);
            throw new RuntimeException("Subscription expired");
        }
        return bctx;
    }
    
    private static Addressing createPushEventMessage(BaseContext bctx,
            Object content)
            throws SOAPException, JAXBException, IOException {
        // Push mode, send the data
        if (!(bctx instanceof EventingContext)) {
            throw new RuntimeException("Context not found");
        }
        final EventingContext ctx = (EventingContext) bctx;
        
        final Addressing msg = new Addressing();
        msg.setAction(Management.EVENT_URI);
        
        if (ctx.getFilter() == null) {
            if (content instanceof Node)
                msg.getBody().appendChild(msg.getBody().getOwnerDocument().importNode((Node)content, true));
            else {
                msg.getXmlBinding().marshal(content, msg.getBody());
            }
        } else {
            final Element item;
            
            // Convert the content to an Element
            if (content instanceof Element) {
                item = (Element) content;
            } else if (content instanceof Document) {
                item = ((Document) content).getDocumentElement();
                // append the Element to the owner document
                // if it has not been done
                // this is critical for XPath filtering to work
                final Document owner = item.getOwnerDocument();
                if (owner.getDocumentElement() == null) {
                    owner.appendChild(item);
                }
            } else {
                Document doc = Management.newDocument();
                try {
                    // TODO: Use a better binding...
                    msg.getXmlBinding().marshal(content, doc);
                } catch (Exception e) {
                    removeContext(null, ctx);
                    final String explanation = "XML Binding marshall failed for object of type: "
                            + content.getClass().getName();
                    throw new InternalErrorFault(SOAP
                            .createFaultDetail(explanation, null,
                            e, null));
                }
                item = doc.getDocumentElement();
            }
            final NodeList filteredContent;
            try {
                filteredContent = ctx.evaluate(item);
            } catch (XPathException xpx) {
                removeContext(null, ctx);
                throw new CannotProcessFilterFault(
                        "Error evaluating XPath: "
                        + xpx.getMessage());
            } catch (Exception ex) {
                removeContext(null, ctx);
                throw new CannotProcessFilterFault(
                        "Error evaluating Filter: "
                        + ex.getMessage());
            }
            if ((filteredContent != null) && (filteredContent.getLength() > 0)) {
                // Then send this instance
                if (filteredContent.item(0).equals(item)) {
                    // Whole node was selected
                    final Document doc = msg.newDocument();
                    doc.appendChild(item);
                    msg.getBody().addDocument(doc);
                } else {
                    // Fragment(s) selected
                    final JAXBElement<MixedDataType> fragment = createXmlFragment(filteredContent);
                    msg.getXmlBinding().marshal(fragment, msg.getBody());
                }
                final String nsURI = item.getNamespaceURI();
                final String nsPrefix = item.getPrefix();
                if (nsPrefix != null && nsURI != null) {
                    msg.getBody().addNamespaceDeclaration(nsPrefix, nsURI);
                }
            } else {
                return null;
            }
        }
        final EndpointReferenceType notifyTo = ctx.getNotifyTo();
        msg.setTo(notifyTo.getAddress().getValue());
        final ReferenceParametersType refparams = notifyTo.getReferenceParameters();
        if (refparams != null) {
            msg.addHeaders(refparams);
        }
        final ReferencePropertiesType refprops = notifyTo.getReferenceProperties();
        if (refprops != null) {
            msg.addHeaders(refprops);
        }
        msg.setMessageId(UUID_SCHEME + UUID.randomUUID().toString());
        
        return msg;
    }
        
    /**
     * Retrieve the Context associated to passed ID, then create
     * the WS-Man request for the provided content.
     */
    public static Addressing createPushEventMessage(UUID id, Object content)
    throws SOAPException, JAXBException, IOException {
        
        BaseContext bctx = retrieveContext(id);
        return createPushEventMessage(bctx, content);
    }
    
    //  TODO: avoid blocking the sender - use a thread pool to send notifications
    public static boolean sendEvent(UUID id, Object content)
    throws SOAPException, JAXBException, IOException {
        
        BaseContext bctx = retrieveContext(id);
        
        boolean result = false;
        
        if (bctx instanceof EnumerationContext) {
            // Pull, add data to iterator
            final EnumerationContext ctx = (EnumerationContext) bctx;
            final EventingIterator iterator = (EventingIterator) ctx.getIterator();
            synchronized (iterator) {
                if (iterator != null) {
                    result = iterator.add(new EnumerationItem(content, null));
                    iterator.notifyAll();
                }
            }
        } else {
             final Addressing msg = createPushEventMessage(bctx, content);
            // Push mode, send the data
            if(msg == null)
                result = false;
            else {
                HttpClient.sendResponse(msg);
                result = true;
            }
            return true;
        }
        return result;
    }
}
