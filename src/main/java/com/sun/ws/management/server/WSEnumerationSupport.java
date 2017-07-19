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
 **Revision 1.10  2008/01/17 15:19:09  denis_rachal
 **Issue number:  151, 152, 153, 154, & 155
 **Obtained from:
 **Submitted by:  cahei (151), jfdenise (152-155)
 **Reviewed by:
 **
 **The following issues have been resolved with this commit:
 **
 **Issue 151: Eventing-Renew request returns without error for an enumeration context from Enumerate
 **Issue 152: Eventing subscription infinite expiration is not well supported
 **Issue 153: RenewResponse is not set when sending back the response
 **Issue 154: Subscribe response expires is not of the same type as the request
 **Issue 155: Expiration scheduling fails after renewal with no expires
 **
 **Unit tests have been appropriately updated to test for these issues.
 **
 **Revision 1.9  2007/12/18 11:55:45  denis_rachal
 **Changes to ensure access to member variables in context are synchronized properly for multi-thread access.
 **
 **Revision 1.8  2007/12/17 15:05:21  denis_rachal
 **Change synchronization of iterator in doPull() to avoid lock wait when releasing subscription during a pull that is blocked.
 **
 **Revision 1.7  2007/12/06 06:43:36  denis_rachal
 **Issue number:  149
 **Obtained from:
 **Submitted by:  stanullo
 **Reviewed by:
 **
 **No immediate response after context expiration. Code added to wake up thread waiting for items. Unit test added to test that this now works and neither the Release or the Pull threads are blocked for any length of time.
 **
 **Revision 1.6  2007/12/05 12:40:37  denis_rachal
 **Incorrect logic used in check for more data on iterator.
 **
 **Revision 1.5  2007/12/05 12:31:51  denis_rachal
 **Memory leak on Optimized enumerations when all elements are selected on enumerate, context wass not deleted.
 **
 **Revision 1.4  2007/11/30 14:32:38  denis_rachal
 **Issue number:  140
 **Obtained from:
 **Submitted by:  jfdenise
 **Reviewed by:
 **
 **WSManAgentSupport and WSEnumerationSupport changed to coordinate their separate threads when handling wsman:OperationTimeout and wsen:MaxTime timeouts. If a timeout now occurs during an enumeration operation the WSEnumerationSupport is notified by the WSManAgentSupport thread. WSEnumerationSupport saves any items collected from the EnumerationIterator in the context so they may be fetched by the client on the next pull. Items are no longer lost on timeouts.
 **
 **Tests were added to correctly test this functionality and older tests were updated to properly test timeout functionality.
 **
 **Additionally some tests were updated to make better use of the XmlBinding object and improve performance on testing.
 **
 **Revision 1.3  2007/11/07 11:15:36  denis_rachal
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
 **Revision 1.2  2007/11/02 14:20:52  denis_rachal
 **Issue number:  144 & 146
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **144: Default expiration timeout for enumerate is never set
 **146: Enhance to allow specifying default expiration per IteratorF
 **
 **Revision 1.1  2007/10/31 12:25:38  jfdenise
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
 * $Id: WSEnumerationSupport.java,v 1.11 2008-06-02 07:20:25 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathException;

import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.Message;
import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.addressing.ActionNotSupportedFault;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.enumeration.FilteringNotSupportedFault;
import com.sun.ws.management.enumeration.InvalidEnumerationContextFault;
import com.sun.ws.management.enumeration.InvalidExpirationTimeFault;
import com.sun.ws.management.TimedOutFault;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.eventing.InvalidMessageFault;
import com.sun.ws.management.server.message.WSEnumerationRequest;
import com.sun.ws.management.server.message.WSEnumerationResponse;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;

/**
 * A helper class that encapsulates some of the arcane logic to allow data
 * sources to be enumerated using the WS-Enumeration protocol.
 *
 * @see WSEnumerationIteratorFactory
 * @see EnumerationIterator
 */
public final class WSEnumerationSupport extends WSEnumerationBaseSupport {
    
    public static final String EXPIRES_DEFAULT = "EnumerationExpiresDefault";
    
    private static final int DEFAULT_ITEM_COUNT = 1;
    private static final int DEFAULT_EXPIRATION_MILLIS = 600000; // 10 minutes
    private static final Duration defaultExpiration;
    private static final Duration defaultMaxTime;
    
    static {
        final Map<String, String> propertySet = new HashMap<String, String>();
        WSManAgentSupport.getProperties(WSManAgentSupport.WISEMAN_PROPERTY_FILE_NAME,
        		propertySet);
        
        // Determine MaxTime default based on OperationTimeout default
        String property = System.getProperty(WSManAgentSupport.OPERATION_TIMEOUT);
        if ((property == null) || (property.length() == 0))
        	property = propertySet.get(WSManAgentSupport.OPERATION_TIMEOUT_DEFAULT);
        if ((property == null) || (property.length() == 0))
            defaultMaxTime = datatypeFactory.newDuration(WSManAgentSupport.DEFAULT_TIMEOUT);
        else {
        	long defTimeout = Long.parseLong(property);
        	if (defTimeout < 0)
        		defTimeout = Long.MAX_VALUE;
            defaultMaxTime = datatypeFactory.newDuration(defTimeout);
        }
        
        // Determine Expiration default from properties file
        property = propertySet.get(EXPIRES_DEFAULT); 
        if ((property == null) || (property.length() == 0))
        	defaultExpiration = datatypeFactory.newDuration(DEFAULT_EXPIRATION_MILLIS);
        else {
        	long defExpires = Long.parseLong(property);
        	if (defExpires < 0)
        		defExpires = Long.MAX_VALUE;
        	defaultExpiration = datatypeFactory.newDuration(defExpires);
        }
    }
    
    private WSEnumerationSupport() {
        // super();
    }
      
    /**
     * Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     * operation.
     *
     * @param handlerContext
     *            The handler context for this request
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     *            request.
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse EnumerateResponse}.
     * @param factory
     *            The iterator factory to use to create the iterator
     *            to use for this enumeration request.
     * @param listener
     *            Will be called when the enumeration is successfully created and
     *            when deleted.
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
    public static void enumerate(final HandlerContext handlerContext,
            final WSEnumerationRequest request,
            final WSEnumerationResponse response,
            final ContextListener listener,
            final WSEnumerationIteratorFactory factory)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
        
        final EnumerationIterator iterator = newIterator(factory,
                handlerContext, request, response);
        
        if (iterator == null) {
            throw new ActionNotSupportedFault();
        }
        
        enumerate(handlerContext, request, response, iterator, listener, null, null);
    }
   
    /**
     * Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     * operation.
     *
     * @param handlerContext
     *            The handler context for this request
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     *            request.
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse EnumerateResponse}.
     * @param iterator
     *            The iterator to use for this enumeration request.
     * @param listener
     *            Will be called when the enumeration is successfully created and
     *            when deleted.
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
    public static void enumerate(final HandlerContext handlerContext,
            final WSEnumerationRequest request,
            final WSEnumerationResponse response,
            final EnumerationIterator iterator,
            final ContextListener listener)
            throws DatatypeConfigurationException, SOAPException,
            JAXBException, FaultException {
    	enumerate(handlerContext, request, response, iterator, listener, null, null);
    }
    
    /**
     * Initiate an
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     * operation.
     *
     * @param handlerContext
     *            The handler context for this request
     * @param request
     *            The incoming SOAP message that contains the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate Enumerate}
     *            request.
     * @param response
     *            The empty SOAP message that will contain the
     *            {@link org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse EnumerateResponse}.
     * @param iterator
     *            The iterator to use for this enumeration request.
     * @param listener
     *            Will be called when the enumeration is successfully created and
     *            when deleted.
     * @param defExpiration 
     *             The default expiration for this request
     *             if none is specified by the request.
     *             If null the system default of 10 minutes is used.           
     * @param maxExpiration
     *             The maximum value a client is allowed to set the Expiration to.
     *             If the requested Expiration exceeds this value, it will be set
     *             to this value. If null infinity is the maximum.
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
    public static void enumerate(final HandlerContext handlerContext,
            final WSEnumerationRequest request,
            final WSEnumerationResponse response,
            final EnumerationIterator iterator,
            final ContextListener listener,
            final Duration defExpiration,
            final Duration maxExpiration)
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
            if (enumerate == null) {
            	iterator.release();
                throw new InvalidMessageFault();
            }
            EnumerationModeType enumerationMode = null;
            boolean optimize = false;
            int maxElements = DEFAULT_ITEM_COUNT;
            
            if (iterator.isFiltered() == false) {
                // We will do the filtering
                filter = createFilter(request);
            }
            
            if (enumerate.getEndTo() != null) {
            	iterator.release();
                throw new UnsupportedFeatureFault(
                        UnsupportedFeatureFault.Detail.ADDRESSING_MODE);
            }
            
            expires = enumerate.getExpires();
            enumerationMode = request.getModeType();
            optimize = request.getOptimize();
            maxElements = request.getMaxElements();
            
        	// Use the WSEnumerationSupport default if none is supplied.
            final Duration enumerationDefault = 
            	(null == defExpiration) ? defaultExpiration : defExpiration;
            
        	final String computeExpires = computeExpiration(expires,
                                                            enumerationDefault,
                                                            maxExpiration);
            
            ctx = createContext(handlerContext, computeExpires, filter,
                                enumerationMode, iterator, listener);
            
            context = initContext(handlerContext, ctx);
            
            if (optimize) {
                
                final List<EnumerationItem> passed = ctx.getItems();
                doPull(handlerContext,
                       request,
                       response,
                       context,
                       ctx,
                       passed,
                       maxElements);
                
        		// Commit the request.
        		try {
        			request.commit();
        		} catch (IllegalStateException ex) {
        			// Too late. Request timed out.
        			throw new TimedOutFault();
        		}
                // place an item count estimate if one was requested
                insertTotalItemCountEstimate(request, response, iterator);
                response.setEnumerateResponse(context.toString(),
                		computeExpires,
                        passed,
                        enumerationMode,
                        ctx.getIterator().hasNext());
                passed.clear();
                if (ctx.getIterator().hasNext() == false) {
    				// remove the context as there is no more data
    				removeContext(handlerContext, context);
                }
            } else {
        		// Commit the request.
        		try {
        			request.commit();
        		} catch (IllegalStateException ex) {
        			// Too late. Request timed out.
        			throw new TimedOutFault();
        		}
                // place an item count estimate if one was requested
                insertTotalItemCountEstimate(request, response, iterator);
                response.setEnumerateResponse(context.toString(), ctx
                        .getExpiration());
            }
        }  catch (InvalidEnumerationContextFault e) {
        	// User did not specify a context, so it must have expired.
            if ((ctx != null) && (ctx.isDeleted() == false)) {
                removeContext(handlerContext, ctx);
            }
            throw new TimedOutFault();
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
            final WSEnumerationRequest request,
            final WSEnumerationResponse response)
            throws SOAPException, JAXBException, FaultException {
        
        assert datatypeFactory != null : UNINITIALIZED;
        
        final Pull pull = request.getPull();
        if (pull == null) {
            throw new InvalidMessageFault();
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
        
        final BigInteger maxElementsBig = pull.getMaxElements();
        final int maxElements;
        if (maxElementsBig == null) {
            maxElements = DEFAULT_ITEM_COUNT;
        } else {
            // NOTE: down casting from BigInteger to Integer
            maxElements = maxElementsBig.intValue();
        }
        
        final List<EnumerationItem> passed = ctx.getItems();
        synchronized (passed) {
            if (ctx.isDeleted()) {
                // Context was deleted while we were waiting. Abort the request.
                throw new InvalidEnumerationContextFault();
            }
        	if (passed.size() < maxElements)
			    doPull(handlerContext, request, response, context, ctx,
					   passed, maxElements);

			// Commit the request.
			try {
				request.commit();
			} catch (IllegalStateException ex) {
				// Too late. Request timed out.
				throw new TimedOutFault();
			}
			// place an item count estimate if one was requested
			insertTotalItemCountEstimate(request, response, ctx.getIterator());

			final int count = ((passed.size() > maxElements) ? maxElements : passed.size());
			final List<EnumerationItem> committed = passed.subList(0, count);
			final boolean more = (count < passed.size())
					|| (ctx.getIterator().hasNext());
			if (more) {
				response.setPullResponse(committed, context.toString(), true,
						ctx.getEnumerationMode());
			} else {
				response.setPullResponse(committed, null, false, ctx.getEnumerationMode());
			}
			committed.clear();
			if (more == false) {
				// remove the context as there is no more data
				removeContext(handlerContext, context);
			}
		}
    }
    
    static EnumerationContext createContext(HandlerContext handlerContext,
            String expires, Filter filter,
            EnumerationModeType enumerationMode, EnumerationIterator iterator,
            ContextListener listener) {
    	
    	final XMLGregorianCalendar expiration = 
    		(expires == null) ? null : initExpiration(expires);
   	        	
        EnumerationContext ctx = new EnumerationContext(expiration,
        		                                        filter,
        		                                        enumerationMode,
                                                        iterator,
                                                        listener);
        return ctx;
    }
    
    private static void doPull(final HandlerContext handlerContext,
            final WSEnumerationRequest request,
            final WSEnumerationResponse response,
            final UUID context,
            final EnumerationContext ctx,
            final List<EnumerationItem> passed,
            final int maxElements)
            throws SOAPException, JAXBException, FaultException {
        
        final EnumerationIterator iterator = ctx.getIterator();
        final GregorianCalendar start = new GregorianCalendar();
        final Duration maxTimeout = request.getMaxTime();
        
        long timeout;
        if (maxTimeout != null) {
            timeout = maxTimeout.getTimeInMillis(start);
        } else {
        	timeout = defaultMaxTime.getTimeInMillis(start);
        }
        final long end = start.getTimeInMillis() + timeout;
        
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
        // Pull now synchronizes on 'passed', so this is not necessary.
        // synchronized (iterator) {
            try {
                // XXX REVISIT,
                // Need to use reflection to remove dependency on EventingExtensions and
                // Enumeration.
                if (iterator instanceof EnumerationPullIterator) {
                    Management mgt;
                    try {
                        mgt = new Management(request.toSOAPMessage());
                    } catch (Exception ex) {
                        throw new SOAPException(ex.toString());
                    }

                    Enumeration en = new Enumeration(mgt);
                    ((EnumerationPullIterator) iterator).startPull(
                            handlerContext, en);
                } else {
                    if(iterator instanceof WSEnumerationPullIterator)
                        ((WSEnumerationPullIterator) iterator).startPull(
                                handlerContext, request);
                }
                
                while ((passed.size() < maxElements) && (iterator.hasNext())) {
                    if (ctx.isDeleted()) {
                        // Context was deleted. Abort the request.
                        throw new InvalidEnumerationContextFault();
                    }
                    
                    // Check for cancellation
                    if (request.isCanceled()) {
                    	throw new TimedOutFault();
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
						long timeLeft = end - new GregorianCalendar().getTimeInMillis();
						while ((timeLeft > 0) && (request.isCanceled() == false) && (ee == null)) {
							try {
								if (ctx.isDeleted()) {
									// Context was deleted while we were waiting
									throw new InvalidEnumerationContextFault();
								}
								// Synchronize the iterator so we can wait on it
								synchronized (iterator) {
								    iterator.wait(timeLeft);
								}
								if (ctx.isDeleted()) {
									// Context was deleted while we were waiting
									throw new InvalidEnumerationContextFault();
								}
								timeLeft = end - new GregorianCalendar().getTimeInMillis();
								if ((timeLeft <=0) || (request.isCanceled()))
									break;
								ee = iterator.next();
							} catch (InterruptedException e) {
								timeLeft = end - new GregorianCalendar().getTimeInMillis();
								continue;
							}
						}
						if (ctx.isDeleted()) {
							// Context was deleted while we were waiting
							throw new InvalidEnumerationContextFault();
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
                                Document doc = Message.newDocument();
                                try {
                                    response.getJAXBContext().createMarshaller().marshal(element,
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
                                    Map<String, String> ns = new HashMap<String, String>();
                                    ns.put(nsPrefix, nsURI);
                                    response.addNamespaceDeclarations(ns);
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
                    Management mgt;
                    try {
                        mgt = new Management(request.toSOAPMessage());
                    } catch (Exception ex) {
                        throw new SOAPException(ex.toString());
                    }
                    // XXX REVISIT,
                    // Need to use reflection to remove dependency on EventingExtensions and
                    // Enumeration.
                    Enumeration en = new Enumeration(mgt);
                    ((EnumerationPullIterator) iterator).endPull(en);
                } else {
                    if(iterator instanceof WSEnumerationPullIterator)
                        ((WSEnumerationPullIterator) iterator).endPull(response);
                }
            }
         // }
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
            final WSEnumerationRequest request,
            final WSEnumerationResponse response)
            throws SOAPException, JAXBException, FaultException {
        
        final Release release = request.getRelease();
        
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
        
        // Check if context is already deleted.
        if (ctx.isDeleted()) {
            throw new InvalidEnumerationContextFault();
        }
        // Make sure this is not an Eventing Pull
        if (ctx instanceof EventingContextPull) {
            // Release is not supported for Eventing Pull
            throw new ActionNotSupportedFault();
        }
        final BaseContext rctx = removeContext(handlerContext, context);
        if (rctx == null) {
            throw new InvalidEnumerationContextFault();
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
    public static Filter createFilter(final WSEnumerationRequest request)
    throws CannotProcessFilterFault, FilteringRequestedUnavailableFault {
        try {
            final org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType enuFilter = request.getEnumerationFilter();
            final DialectableMixedDataType enxFilter = request
                    .getWsmanEnumerationFilter();
            
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
    
    public static NamespaceMap getNamespaceMap(final WSEnumerationRequest request) {
        final NamespaceMap nsMap;
        final SOAPBody body;
        try {
            body = request.toSOAPMessage().getSOAPBody();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible to convert to SOAPMessage");
        }
        
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
    
    private static void insertTotalItemCountEstimate(final WSEnumerationRequest request,
            final WSEnumerationResponse response, final EnumerationIterator iterator)
            throws SOAPException, JAXBException {
        // place an item count estimate if one was requested
        if (request.getRequestTotalItemsCountEstimate() != null) {
            final int estimate = iterator.estimateTotalItems();
            if (estimate < 0) {
                // estimate not available
                response.setTotalItemsCountEstimate(null);
            } else {
                response.setTotalItemsCountEstimate(new BigInteger(Integer
                        .toString(estimate)));
            }
        }
    }
    
    @SuppressWarnings("static-access")
    synchronized static EnumerationIterator newIterator(
            final Object factory,
            final HandlerContext context, final WSEnumerationRequest request,
            final WSEnumerationResponse response) throws SOAPException, JAXBException {
        
        if (factory == null) {
            return null;
        }
        final Filter filter = createFilter(request);
        final Boolean includeItem;
        final Boolean includeEPR;
        
        final EnumerationExtensions.Mode mode = request.getMode();
        if (mode == null) {
            includeItem = true;
            includeEPR = false;
        } else {
            if (mode.equals(EnumerationExtensions.Mode.EnumerateEPR)) {
                includeItem = (filter == null) ? false : true;
                includeEPR = true;
            } else if (mode.equals(EnumerationExtensions.Mode.EnumerateObjectAndEPR)) {
                includeItem = true;
                includeEPR = true;
            } else {
                throw new UnsupportedFeatureFault(
                        UnsupportedFeatureFault.Detail.ENUMERATION_MODE);
            }
        }
        
        if(factory instanceof IteratorFactory) {
            IteratorFactory fact = (IteratorFactory) factory;
            Management req;
            try {
                req = new Management(request.toSOAPMessage());
            } catch (Exception ex) {
                throw new SOAPException(ex.toString());
            }
            Enumeration en = new Enumeration(req);
            return fact.newIterator(context, en, Message.getDocumentBuilder(), includeItem,
                    includeEPR);
        } else {
            return ((WSEnumerationIteratorFactory) factory).newIterator(context, request, includeItem,
                    includeEPR);
        }
    }
}
