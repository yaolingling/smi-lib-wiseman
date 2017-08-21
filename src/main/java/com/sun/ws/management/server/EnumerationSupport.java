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
 **$Log: EnumerationSupport.java,v $
 **Revision 1.51  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EnumerationSupport.java,v 1.51 2007/05/30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathException;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.addressing.ActionNotSupportedFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.enumeration.FilteringNotSupportedFault;
import com.sun.ws.management.enumeration.InvalidEnumerationContextFault;
import com.sun.ws.management.enumeration.InvalidExpirationTimeFault;
import com.sun.ws.management.enumeration.TimedOutFault;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.eventing.InvalidMessageFault;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;

/**
 * A helper class that encapsulates some of the arcane logic to allow data
 * sources to be enumerated using the WS-Enumeration protocol.
 *
 * @see IteratorFactory
 * @see EnumerationIterator
 */
public final class EnumerationSupport extends BaseSupport {

    private static final int DEFAULT_ITEM_COUNT = 1;
    private static final int DEFAULT_EXPIRATION_MILLIS = 60000;
    private static final long DEFAULT_MAX_TIMEOUT_MILLIS = 300000;
    private static final Map<String, IteratorFactory> registeredIterators = new HashMap<String, IteratorFactory>();

    private static Duration defaultExpiration = null;

    static {
        defaultExpiration = datatypeFactory.newDuration(DEFAULT_EXPIRATION_MILLIS);
    }

    private EnumerationSupport() {
        // super();
    }

    /**
     * Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     * operation. Handlers calling this method must first register an
     * {@link IteratorFactory}.
     *
     * @see #registerIteratorFactory(String, IteratorFactory)
     *
     * @param context
     *            the handler context for this request
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     *            request.
     *
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse EnumerateResponse}.
     *
     * @throws FilteringNotSupportedFault
     *             if filtering is not supported.
     *
     * @throws InvalidExpirationTimeFault
     *             if the expiration time specified in the request is
     *             syntactically-invalid or is in the past.
     *
     * @throws FaultException
     * @throws DatatypeConfigurationException
     * @throws SOAPException
     * @throws JAXBException
     */
    public static void enumerate(final HandlerContext context,
            final Enumeration request,
            final Enumeration response)
            throws FaultException, DatatypeConfigurationException,
            SOAPException, JAXBException {
        final EnumerationIterator iterator = newIterator(context, request, response);
        if (iterator == null) {
            throw new ActionNotSupportedFault();
        }
        enumerate(context, request, response, iterator, null);
    }

    /**
     * Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     * operation. Handlers calling this method must first register an
     * {@link IteratorFactory}.
     *
     * @see #registerIteratorFactory(String, IteratorFactory)
     *
     * @param context
     *            the handler context for this request
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     *            request.
     *
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse EnumerateResponse}.
     *
     * @param listener will be called when the enumeration is successfully created and
     *           when deleted.
     *
     * @throws FilteringNotSupportedFault
     *             if filtering is not supported.
     *
     * @throws InvalidExpirationTimeFault
     *             if the expiration time specified in the request is
     *             syntactically-invalid or is in the past.
     *
     * @throws FaultException
     * @throws DatatypeConfigurationException
     * @throws SOAPException
     * @throws JAXBException
     */
    public static void enumerate(final HandlerContext context,
            final Enumeration request,
            final Enumeration response,
            final ContextListener listener)
            throws FaultException, DatatypeConfigurationException,
            SOAPException, JAXBException {
        final EnumerationIterator iterator = newIterator(context, request, response);
        if (iterator == null) {
            throw new ActionNotSupportedFault();
        }
        enumerate(context, request, response, iterator, listener);
    }

    private static void enumerate(final HandlerContext handlerContext,
            final Enumeration request,
            final Enumeration response,
            final EnumerationIterator iterator,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        EnumerationContext ctx = null;
        UUID context = null;

        try {
            assert datatypeFactory != null : UNINITIALIZED;
            assert defaultExpiration != null : UNINITIALIZED;

            String expires = null;
            Filter filter = null;

            final Enumerate enumerate = request.getEnumerate();
            EnumerationModeType enumerationMode = null;
            boolean optimize = false;
            int maxElements = DEFAULT_ITEM_COUNT;

            if (enumerate == null) {
                // see if this is a pull event mode subscribe request
                final EventingExtensions evtxRequest = new EventingExtensions(request);
                final Subscribe subscribe = evtxRequest.getSubscribe();
                if (subscribe == null) {
                    throw new InvalidMessageFault();
                }
                if (iterator.isFiltered() == false) {
                    // We will do the filtering
                    filter = EventingSupport.createFilter(evtxRequest);
                }
                expires = subscribe.getExpires();

                if (subscribe.getEndTo() != null) {
                    throw new UnsupportedFeatureFault(
                            UnsupportedFeatureFault.Detail.ADDRESSING_MODE);
                }
            } else {
                if (iterator.isFiltered() == false) {
                    // We will do the filtering
                    filter = createFilter(request);
                }
                if (enumerate.getEndTo() != null) {
                    throw new UnsupportedFeatureFault(
                            UnsupportedFeatureFault.Detail.ADDRESSING_MODE);
                }
                final EnumerationExtensions ext = new EnumerationExtensions(request);
                expires = enumerate.getExpires();
                enumerationMode = ext.getModeType();
                optimize = ext.getOptimize();
                maxElements = ext.getMaxElements();
            }

            XMLGregorianCalendar expiration = initExpiration(expires);
            if (expiration == null) {
                final GregorianCalendar now = new GregorianCalendar();
                expiration = datatypeFactory.newXMLGregorianCalendar(now);
                expiration.add(defaultExpiration);
            }

            ctx = new EnumerationContext(expiration, filter, enumerationMode,
                    iterator, listener);

            context = initContext(handlerContext, ctx);
            if (enumerate == null) {
                // this is a pull event mode subscribe request
                final EventingExtensions evtxRequest = new EventingExtensions(request);
                final EventingExtensions evtxResponse = new EventingExtensions(response);
                evtxResponse.setSubscribeResponse(EventingSupport
                        .createSubscriptionManagerEpr(evtxRequest,
                        evtxResponse, context), ctx.getExpiration(),
                        context.toString());
            } else {
                if (optimize) {
                    final Duration maxTime = new Management(request).getTimeout();
                    final List<EnumerationItem> passed = new ArrayList<EnumerationItem>();
                    final boolean more = doPull(handlerContext,
                            request,
                            response,
                            context,
                            ctx,
                            maxTime,
                            passed,
                            maxElements);

                    final EnumerationExtensions enxResponse = new EnumerationExtensions(
                            response);
                    enxResponse.setEnumerateResponse(context.toString(),
                            ctx.getExpiration(),
                            passed,
                            enumerationMode,
                            more);
                } else {
                    // place an item count estimate if one was requested
                    insertTotalItemCountEstimate(request, response, iterator);
                    response.setEnumerateResponse(context.toString(), ctx
                            .getExpiration());
                }
            }
        } catch (TimedOutFault e) {
            // Do not delete the context for timeouts
            throw e;
        } catch (FaultException e) {
            if ((ctx != null) && (ctx.isDeleted() == false)) {
                removeContext(handlerContext, ctx);
            }
            throw e;
        } catch (Throwable t) {
            if ((ctx != null) && (ctx.isDeleted() == false)) {
                removeContext(handlerContext, ctx);
            }
            throw new InternalErrorFault(t);
        }
    }

    /**
     * Handle a {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Pull Pull}
     * request.
     *
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Pull Pull}
     *            request.
     *
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse PullResponse}.
     *
     * @throws InvalidEnumerationContextFault
     *             if the supplied context is missing, is not understood or is
     *             not found because it has expired or the server has been
     *             restarted.
     *
     * @throws TimedOutFault
     *             if the data source fails to provide the items to be returned
     *             within the specified
     *             {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Pull#getMaxTime timeout}.
     */
    public static void pull(final HandlerContext handlerContext,
            final Enumeration request,
            final Enumeration response)
            throws SOAPException, JAXBException, FaultException {

        assert datatypeFactory != null : UNINITIALIZED;

        final Pull pull = request.getPull();
        if (pull == null) {
            throw new InvalidEnumerationContextFault();
        }

        final BigInteger maxChars = pull.getMaxCharacters();
        if (maxChars != null) {
            // TODO: add support for maxChars
            throw new UnsupportedFeatureFault(
                    UnsupportedFeatureFault.Detail.MAX_ENVELOPE_SIZE);
        }

        final EnumerationContextType contextType = pull.getEnumerationContext();
        final UUID context = extractContext(contextType);
        final EnumerationContext ctx = (EnumerationContext) getContext(context);
        if (ctx == null) {
            throw new InvalidEnumerationContextFault();
        }

        if (ctx.isDeleted()) {
            throw new InvalidEnumerationContextFault();
        }
        final GregorianCalendar now = new GregorianCalendar();
        final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);
        if (ctx.isExpired(nowXml)) {
            removeContext(handlerContext, context);
            throw new InvalidEnumerationContextFault();
        }

        final BigInteger maxElementsBig = pull.getMaxElements();
        final int maxElements;
        if (maxElementsBig == null) {
            maxElements = DEFAULT_ITEM_COUNT;
        } else {
            // NOTE: downcasting from BigInteger to int
            maxElements = maxElementsBig.intValue();
        }

        Duration maxTime = new Management(request).getTimeout();
        if (maxTime == null) {
            maxTime = pull.getMaxTime();
        }
        final List<EnumerationItem> passed = new ArrayList<EnumerationItem>();
        final boolean more = doPull(handlerContext, request, response, context,
                ctx, maxTime, passed, maxElements);

        final EnumerationExtensions wsmanResponse = new EnumerationExtensions(
                response);
        if (more) {
            wsmanResponse.setPullResponse(passed, context.toString(), true,
                    ctx.getEnumerationMode());
        } else {
            wsmanResponse.setPullResponse(passed, null, false,
                    ctx.getEnumerationMode());
        }
    }

    private static boolean doPull(final HandlerContext handlerContext,
            final Enumeration request,
            final Enumeration response,
            final UUID context,
            final EnumerationContext ctx,
            final Duration maxTimeout,
            final List<EnumerationItem> passed,
            final int maxElements)
            throws SOAPException, JAXBException, FaultException {

        final EnumerationIterator iterator = ctx.getIterator();
        final GregorianCalendar start = new GregorianCalendar();

        long timeout = DEFAULT_MAX_TIMEOUT_MILLIS;
        if (maxTimeout != null) {
            timeout = maxTimeout.getTimeInMillis(start);
        }
        final long end = start.getTimeInMillis() + timeout;

        final SOAPEnvelope env = response.getEnvelope();

        // Check the enumeration mode
        boolean includeItem = false;
        boolean includeEPR = false;
        final EnumerationModeType mode = ctx.getEnumerationMode();
        if (mode == null) {
            includeItem = true;
            includeEPR = false;
        } else {
            final String modeString = mode.value();
            if (modeString.equals(EnumerationExtensions.Mode.EnumerateEPR.toString())) {
                includeItem = false;
                includeEPR = true;
            } else if (modeString
                    .equals(EnumerationExtensions.Mode.EnumerateObjectAndEPR
                    .toString())) {
                includeItem = true;
                includeEPR = true;
            } else {
                removeContext(handlerContext, context);
                throw new UnsupportedFeatureFault(
                        UnsupportedFeatureFault.Detail.ENUMERATION_MODE);
            }
        }
        Boolean fragmentCheck = null;

        // Synchronize on the iterator
        synchronized (iterator) {
            try {
                if (iterator instanceof EnumerationPullIterator) {
                    ((EnumerationPullIterator) iterator).startPull(
                            handlerContext, request);
                }
                while ((passed.size() < maxElements) && (iterator.hasNext())) {
                    if (ctx.isDeleted()) {
                        // Context was deleted. Abort the request.
                        throw new InvalidEnumerationContextFault();
                    }

                    // Check for a timeout
                    if (new GregorianCalendar().getTimeInMillis() >= end) {
                        if (passed.size() == 0) {
                            // timed out with no data
                            throw new TimedOutFault();
                        } else {
                            // timed out with data
                            break;
                        }
                    }
                    EnumerationItem ee = iterator.next();
                    if (ctx.isDeleted()) {
                        // Context was deleted while we were waiting
                        throw new InvalidEnumerationContextFault();
                    }
                    if ((ee == null) && (passed.size() == 0)) {
                        // wait for some data to arrive
                        long timeLeft = end
                                - new GregorianCalendar().getTimeInMillis();
                        while ((timeLeft > 0) && (ee == null)) {
                            try {
                                iterator.wait(timeLeft);
                                if (ctx.isDeleted()) {
                                    // Context was deleted while we were waiting
                                    throw new InvalidEnumerationContextFault();
                                }
                                ee = iterator.next();
                                timeLeft = end
                                        - new GregorianCalendar()
                                        .getTimeInMillis();
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                    if (ee == null) {
                        if (passed.size() == 0) {
                            throw new TimedOutFault();
                        } else {
                            break;
                        }
                    }

                    // apply filter, if any
                    //
                    // retrieve the document element from the enumeration
                    // element
                    final Object element = ee.getItem();

                    // Check if request matches data provided:
                    // data only, EPR only, or data and EPR
                    if ((includeEPR == true)
                    && (ee.getEndpointReference() == null)) {
                        removeContext(handlerContext, context);
                        throw new UnsupportedFeatureFault(
                                UnsupportedFeatureFault.Detail.INVALID_VALUES);
                    }
                    if ((includeItem == true) && (element == null)) {
                        removeContext(handlerContext, context);
                        throw new UnsupportedFeatureFault(
                                UnsupportedFeatureFault.Detail.INVALID_VALUES);
                    }
                    if (iterator.isFiltered()) {
                        passed.add(ee);
                    } else {
                        if (element != null) {
                            final Element item;

                            if (element instanceof Element) {
                                item = (Element) element;
                            } else if (element instanceof Document) {
                                item = ((Document) element)
                                .getDocumentElement();
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
                                    response.getXmlBinding().marshal(element,
                                            doc);
                                } catch (Exception e) {
                                    removeContext(handlerContext, context);
                                    final String explanation = "XML Binding marshall failed for object of type: "
                                            + element.getClass().getName();
                                    throw new InternalErrorFault(SOAP
                                            .createFaultDetail(explanation,
                                            null, e, null));
                                }
                                item = doc.getDocumentElement();
                            }
                            final NodeList result;
                            try {
                                result = ctx.evaluate(item);
                            } catch (XPathException xpx) {
                                removeContext(handlerContext, context);
                                throw new CannotProcessFilterFault(
                                        "Error evaluating XPath: "
                                        + xpx.getMessage());
                            } catch (Exception ex) {
                                removeContext(handlerContext, context);
                                throw new CannotProcessFilterFault(
                                        "Error evaluating Filter: "
                                        + ex.getMessage());
                            }
                            if ((result != null) && (result.getLength() > 0)) {
                                // Then add this instance
                                if (fragmentCheck == null) {
                                    // Only check this once
                                    // If 'result' is same as the 'item'
                                    // then this is not a fragment selection
                                    fragmentCheck = new Boolean(result.item(0)
                                    .equals(item));
                                }
                                if (fragmentCheck == true) {
                                    // Whole node was selected
                                    passed.add(ee);
                                } else {
                                    // Fragment(s) selected
                                    JAXBElement<MixedDataType> fragment = createXmlFragment(result);
                                    EnumerationItem fragmentItem = new EnumerationItem(
                                            fragment, ee.getEndpointReference());
                                    passed.add(fragmentItem);
                                }
                                final String nsURI = item.getNamespaceURI();
                                final String nsPrefix = item.getPrefix();
                                if (nsPrefix != null && nsURI != null) {
                                    env
                                            .addNamespaceDeclaration(nsPrefix,
                                            nsURI);
                                }
                            }
                        } else {
                            if (EnumerationModeType.ENUMERATE_EPR.equals(mode)) {
                                if (ee.getEndpointReference() != null)
                                    passed.add(ee);
                            }
                        }
                    }
                }
            } finally {
                if (iterator instanceof EnumerationPullIterator) {
                    ((EnumerationPullIterator) iterator).endPull(response);
                }
            }

            // place an item count estimate if one was requested
            insertTotalItemCountEstimate(request, response, ctx.getIterator());

            if (iterator.hasNext() == false) {
                // remove the context -
                // a subsequent release will fault with an invalid context
                removeContext(handlerContext, context);
                return false;
            }
            return iterator.hasNext();
        }
    }

    /**
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Release Release} an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumeration}
     * in progress.
     *
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Release Release}
     *            request.
     *
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Release Release}
     *            response.
     *
     * @throws InvalidEnumerationContextFault
     *             if the supplied context is missing, is not understood or is
     *             not found because it has expired or the server has been
     *             restarted.
     */
    public static void release(final HandlerContext handlerContext,
            final Enumeration request,
            final Enumeration response)
            throws SOAPException, JAXBException, FaultException {

        final Release release = request.getRelease();
        if (release == null) {
            // this might be a pull-mode unsubscribe request
            final Eventing evt = new Eventing(request);
            final Unsubscribe unsub = evt.getUnsubscribe();
            if (unsub != null) {
                EventingSupport.unsubscribe(handlerContext, evt, new Eventing(
                        response));
                return;
            }
            throw new InvalidEnumerationContextFault();
        }
        final EnumerationContextType contextType = release
                .getEnumerationContext();
        if (contextType == null) {
            throw new InvalidEnumerationContextFault();
        }
        final UUID context = extractContext(contextType);
        final BaseContext ctx = getContext(context);
        if (ctx == null) {
            throw new InvalidEnumerationContextFault();
        }
        if ((ctx instanceof EnumerationContext) == false) {
            throw new InvalidEnumerationContextFault();
        }

        // Set single thread use of this context
        // synchronized (ctx) {
        if (ctx.isDeleted()) {
            throw new InvalidEnumerationContextFault();
        }
        // Make sure this is not an Eventing Pull
        EnumerationIterator iterator = ((EnumerationContext) ctx).getIterator();
        if (iterator instanceof EventingIterator) {
            // Release is not supported for Eventing Pull
            throw new ActionNotSupportedFault();
        }
        final BaseContext rctx = removeContext(handlerContext, context);
        if (rctx == null) {
            throw new InvalidEnumerationContextFault();
        }
        // }
    }

    /**
     * Utility method to create an EPR for accessing individual elements of an
     * enumeration directly.
     *
     * @param address
     *            The transport address of the service.
     *
     * @param resource
     *            The resource being addressed.
     *
     * @param selectorMap
     *            Selectors used to identify the resource. Optional.
     */
    public static EndpointReferenceType createEndpointReference(final String address,
            final String resource,
            final Map<String, String> selectorMap) {

        final ReferenceParametersType refp = Addressing.FACTORY
                .createReferenceParametersType();

        final AttributableURI attributableURI = Management.FACTORY
                .createAttributableURI();
        attributableURI.setValue(resource);
        refp.getAny()
        .add(Management.FACTORY.createResourceURI(attributableURI));

        if (selectorMap != null) {
            final SelectorSetType selectorSet = Management.FACTORY
                    .createSelectorSetType();
            final Iterator<Entry<String, String>> si = selectorMap.entrySet()
            .iterator();
            while (si.hasNext()) {
                final Entry<String, String> entry = si.next();
                final SelectorType selector = Management.FACTORY
                        .createSelectorType();
                selector.setName(entry.getKey());
                selector.getContent().add(entry.getValue());
                selectorSet.getSelector().add(selector);
            }
            refp.getAny()
            .add(Management.FACTORY.createSelectorSet(selectorSet));
        }

        return Addressing.createEndpointReference(address, null, refp, null,
                null);
    }

    /**
     * Create a Filter from an Enumeration request
     *
     * @return Returns a Filter object if a filter exists in the request,
     *         otherwise null.
     * @throws CannotProcessFilterFault,
     *             FilteringRequestedUnavailableFault, InternalErrorFault
     */
    public static Filter createFilter(final Enumeration request)
    throws CannotProcessFilterFault, FilteringRequestedUnavailableFault {
        try {
            final EnumerationExtensions enxRequest = new EnumerationExtensions(
                    request);
            final Enumerate enumerate = enxRequest.getEnumerate();
            final org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType enuFilter = enumerate
                    .getFilter();
            final DialectableMixedDataType enxFilter = enxRequest
                    .getWsmanFilter();

            if ((enuFilter == null) && (enxFilter == null)) {
                return null;
            }
            if ((enuFilter != null) && (enxFilter != null)) {
                // Both are not allowed. Throw an exception
                throw new CannotProcessFilterFault(
                        SOAP
                        .createFaultDetail(
                        "Both wsen:Filter and wsman:Filter were specified in the request. Only one is allowed.",
                        null, null, null));
            }

            // This is the namespaces used in the filter expression itself
            final NamespaceMap nsMap = getNamespaceMap(request);

            if (enxFilter != null)
                return createFilter(enxFilter.getDialect(), enxFilter
                        .getContent(), nsMap);
            else
                return createFilter(enuFilter.getDialect(), enuFilter
                        .getContent(), nsMap);
        } catch (SOAPException e) {
            throw new InternalErrorFault(e);
        } catch (JAXBException e) {
            throw new InternalErrorFault(e);
        }
    }

    private static NamespaceMap getNamespaceMap(final Enumeration request) {
        final NamespaceMap nsMap;
        final SOAPBody body = request.getBody();

        NodeList wsmanFilter = body.getElementsByTagNameNS(
                EnumerationExtensions.FILTER.getNamespaceURI(),
                EnumerationExtensions.FILTER.getLocalPart());
        NodeList enumFilter = body.getElementsByTagNameNS(Enumeration.FILTER
                .getNamespaceURI(), Enumeration.FILTER.getLocalPart());
        if ((wsmanFilter != null) && (wsmanFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(wsmanFilter.item(0));
        } else if ((enumFilter != null) && (enumFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(enumFilter.item(0));
        } else {
            NodeList enumElement = body.getElementsByTagNameNS(
                    Enumeration.ENUMERATE.getNamespaceURI(),
                    Enumeration.ENUMERATE.getLocalPart());
            nsMap = new NamespaceMap(enumElement.item(0));
        }
        return nsMap;
    }

    private static UUID extractContext(final EnumerationContextType contextType)
    throws FaultException {

        if (contextType == null) {
            throw new InvalidEnumerationContextFault();
        }

        final String contextString = (String) contextType.getContent().get(0);
        UUID context;
        try {
            context = UUID.fromString(contextString);
        } catch (IllegalArgumentException argex) {
            throw new InvalidEnumerationContextFault();
        }

        return context;
    }

    private static void insertTotalItemCountEstimate(final Enumeration request,
            final Enumeration response, final EnumerationIterator iterator)
            throws SOAPException, JAXBException {
        // place an item count estimate if one was requested
        final EnumerationExtensions enx = new EnumerationExtensions(request);
        if (enx.getRequestTotalItemsCountEstimate() != null) {
            final EnumerationExtensions rx = new EnumerationExtensions(response);
            final int estimate = iterator.estimateTotalItems();
            if (estimate < 0) {
                // estimate not available
                rx.setTotalItemsCountEstimate(null);
            } else {
                rx.setTotalItemsCountEstimate(new BigInteger(Integer
                        .toString(estimate)));
            }
        }
    }

    /**
     * Add an iterator factory to EnumerationSupport.
     *
     * @param resourceURI
     *            ResourceURI for which this iterator factory is to be used to
     *            fufill Enumeration requests.
     * @param iteratorFactory
     *            The Iterator Factory that creates
     *            <code>EnumerationIterator</code> objects that are used by
     *            EnumerationSupport to fufill Enumeration requests. If a
     *            factory is already registered it will be overwritten with the
     *            specified factory.
     */
    public synchronized static void registerIteratorFactory(String resourceURI,
            IteratorFactory iteratorFactory) throws Exception {
        registeredIterators.put(resourceURI, iteratorFactory);
    }

    /**
     * Gets an IteratorFactory for the specified resource URI.
     *
     * @param resourceURI
     *            the URI associated with the IteratorFactory
     * @return the IteratorFactory if one is registered, otherwise null
     */
    public synchronized static IteratorFactory getIteratorFactory(
            String resourceURI) {
        return registeredIterators.get(resourceURI);
    }

    @SuppressWarnings("static-access")
    private synchronized static EnumerationIterator newIterator(
            final HandlerContext context, final Enumeration request,
            final Enumeration response) throws SOAPException, JAXBException {
        final Management mgmt = new Management(request);
        final IteratorFactory factory = registeredIterators.get(mgmt
                .getResourceURI());
        if (factory == null) {
            return null;
        }
        final DocumentBuilder db = response.getDocumentBuilder();
        final Boolean includeItem;
        final Boolean includeEPR;

        final EnumerationExtensions ext = new EnumerationExtensions(request);
        final EnumerationModeType mode = ext.getModeType();
        if (mode == null) {
            includeItem = true;
            includeEPR = false;
        } else {
            final String modeString = mode.value();
            if (modeString.equals(EnumerationExtensions.Mode.EnumerateEPR
                    .toString())) {
                includeItem = false;
                includeEPR = true;
            } else if (modeString
                    .equals(EnumerationExtensions.Mode.EnumerateObjectAndEPR
                    .toString())) {
                includeItem = true;
                includeEPR = true;
            } else {
                throw new UnsupportedFeatureFault(
                        UnsupportedFeatureFault.Detail.ENUMERATION_MODE);
            }
        }
        return factory.newIterator(context, request, db, includeItem,
                includeEPR);
    }
}
