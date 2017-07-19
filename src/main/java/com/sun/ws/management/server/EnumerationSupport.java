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
 **Revision 1.55  2007/11/02 14:20:52  denis_rachal
 **Issue number:  144 & 146
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **144: Default expiration timeout for enumerate is never set
 **146: Enhance to allow specifying default expiration per IteratorF
 **
 **Revision 1.54  2007/10/31 12:25:17  jfdenise
 **Split between new support and previous one.
 **
 **Revision 1.53  2007/10/30 09:27:30  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.52  2007/10/02 10:43:43  jfdenise
 **Fix for bug ID 134, Enumeration Iterator look up is static
 **Applied to Enumeration and Eventing
 **
 **Revision 1.51  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EnumerationSupport.java,v 1.56 2008-01-17 15:19:09 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import com.sun.ws.management.server.message.SAAJMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.addressing.ActionNotSupportedFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.FilteringNotSupportedFault;
import com.sun.ws.management.enumeration.InvalidEnumerationContextFault;
import com.sun.ws.management.enumeration.InvalidExpirationTimeFault;
import com.sun.ws.management.enumeration.TimedOutFault;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.eventing.InvalidMessageFault;
import com.sun.ws.management.soap.FaultException;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

/**
 * A helper class that encapsulates some of the arcane logic to allow data
 * sources to be enumerated using the WS-Enumeration protocol.
 *
 * @see IteratorFactory
 * @see EnumerationIterator
 */
public final class EnumerationSupport extends WSEnumerationBaseSupport {
    
    private static final Map<String, IteratorFactory> registeredIterators = new HashMap<String, IteratorFactory>();
    
    private EnumerationSupport() {
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
        enumerate(context, request, response, null);
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
        final Management mgmt = new Management(request);
        final IteratorFactory factory = registeredIterators.get(mgmt
                .getResourceURI());
        
        enumerate(context, request, response, listener, factory);
    }
    
    public static void enumerate(final HandlerContext handlerContext,
            final Enumeration request,
            final Enumeration response,
            final ContextListener listener,
            final IteratorFactory factory)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        SAAJMessage req = new SAAJMessage(new Management(request));
        SAAJMessage resp = new SAAJMessage(new Management(response));
        final EnumerationIterator iterator =  WSEnumerationSupport.newIterator(factory,
                handlerContext, req, resp);
        
        if (iterator == null) {
            throw new ActionNotSupportedFault();
        }
        
        final Enumerate enumerate = request.getEnumerate();
        if (enumerate == null) {
        	throw new InvalidMessageFault();
        } else
            WSEnumerationSupport.enumerate(handlerContext, req, resp, iterator, listener);
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
        SAAJMessage req = new SAAJMessage(new Management(request));
        SAAJMessage resp = new SAAJMessage(new Management(response));
        WSEnumerationSupport.pull(handlerContext, req, resp);
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
            final Eventing evt = new Eventing((Addressing)request);
            final Unsubscribe unsub = evt.getUnsubscribe();
            if (unsub != null) {
                EventingSupport.unsubscribe(handlerContext, evt, new Eventing(
                        (Addressing)response));
                return;
            }
            throw new InvalidEnumerationContextFault();
        } else {
            SAAJMessage req = new SAAJMessage(new Management(request));
            SAAJMessage resp = new SAAJMessage(new Management(response));
            WSEnumerationSupport.release(handlerContext, req, resp);
        }
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
            return WSEnumerationSupport.createFilter(new SAAJMessage(new Management(request)));
        } catch(SOAPException ex) {
            throw new InternalErrorFault(ex);
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
}
