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
 **Revision 1.23  2008/05/16 12:50:29  jfdenise
 **It appears that we could map JAXBException better than InternalError. InvalidMessageInformationHeader is much more appropriate.
 **
 **Revision 1.22  2007/11/30 14:32:38  denis_rachal
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
 **Revision 1.21  2007/11/09 12:33:34  denis_rachal
 **Performance enhancements that better reuse the XmlBinding.
 **
 **Revision 1.20  2007/10/31 11:59:24  jfdenise
 **Faulty maxEnvelopSize computation linked to recent putback
 **
 **Revision 1.19  2007/10/31 09:53:55  denis_rachal
 **Fixed error where incorrect fault is returned when setting MaxEnvelopeSize too small. Additionally added performance enhancement to not computer MaxEnvelope size if it is not specified in the request, or it is set to the maximum.
 **
 **Revision 1.18  2007/10/30 09:27:47  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.17  2007/06/15 12:13:20  jfdenise
 **Cosmetic change. Make OPERATION_TIMEOUT_DEFAULT public and added a trace.
 **
 **Revision 1.16  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSManAgent.java,v 1.24 2008-07-17 13:30:55 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Source;

import org.xml.sax.SAXException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import com.sun.ws.management.AccessDeniedFault;
import com.sun.ws.management.EncodingLimitFault;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.Message;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.addressing.InvalidMessageInformationHeaderFault;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.message.SAAJMessage;
import com.sun.ws.management.server.message.WSManagementRequest;
import com.sun.ws.management.server.message.WSManagementResponse;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.transport.HttpClient;

/**
 * WS-MAN agent decoupled from transport. Can be used in Servlet / JAX-WS / ...
 * context.
 *
 */

public abstract class WSManAgent extends WSManAgentSupport {
    private static final Logger LOG = Logger.getLogger(WSManAgent.class.getName());
    private static final String UUID_SCHEME = "uuid:";
    private static final long MIN_ENVELOPE_SIZE = 8192;
    
    private class RequestDispatcherWrapper extends WSManRequestDispatcher {
        private RequestDispatcher dispatcher;
        RequestDispatcherWrapper(RequestDispatcher dispatcher) throws JAXBException, SOAPException {
            super(new SAAJMessage(dispatcher.request), 
                  new SAAJMessage(dispatcher.response), 
                    dispatcher.context);
            this.dispatcher = dispatcher;
        }
        
        public Object call() throws Exception {
            return new SAAJMessage((Management)dispatcher.call());
        }
    }
    
    protected WSManAgent() throws SAXException {
         super();
    }
    protected WSManAgent(Map<String,String> wisemanConf, Source[] customSchemas,
            Map<String,String> bindingConf)
            throws SAXException {
        super(wisemanConf, customSchemas, bindingConf);
    }
    protected WSManRequestDispatcher createDispatcher(WSManagementRequest request,
            WSManagementResponse response,
            HandlerContext context) throws Exception {
    	final Management req = new Management(request.toSOAPMessage());
    	req.setXmlBinding(getXmlBinding());
        return new RequestDispatcherWrapper(createDispatcher(req, context));
    }
    
    public long getValidEnvelopeSize(Management request) throws JAXBException, SOAPException {
        long maxEnvelopeSize = getEnvelopeSize(new SAAJMessage((request)));
         if(maxEnvelopeSize < MIN_ENVELOPE_SIZE)
            maxEnvelopeSize =  Long.MAX_VALUE;
        return maxEnvelopeSize;
    }
    
    /**
     * Hook your own dispatcher
     * @param agent
     */
    abstract protected RequestDispatcher createDispatcher(final Management request,
            final HandlerContext context) throws SOAPException, JAXBException,
            IOException;
    
    /**
     * Agent request handling entry point. Return a Message due to Identify reply.
     */
    public Message handleRequest(final Management request, final HandlerContext context) {
        try {
            SAAJMessage req = new SAAJMessage(request);
            SAAJMessage resp = new SAAJMessage(new Management());
            
            WSManagementResponse response = handleRequest(req, resp, context);
            Management saajResponse = new Management(response.toSOAPMessage());
            saajResponse.setXmlBinding(getXmlBinding());
            fillReturnAddress(request, saajResponse);
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Request / Response content type " +
                        request.getContentType());
            saajResponse.setContentType(request.getContentType());
            
            Message ret = handleResponse(saajResponse, getValidEnvelopeSize(request));
            return ret;
        }catch(Exception ex) {
            try {
                Management response = new Management();
                response.setXmlBinding(request.getXmlBinding());
                if(ex instanceof SecurityException)
                    response.setFault(new AccessDeniedFault());
                else
                    if(ex instanceof FaultException)
                        response.setFault((FaultException)ex);
                    else
                        if(ex instanceof JAXBException)
                            response.setFault(new 
                                    InvalidMessageInformationHeaderFault(ex.
                                    toString()));
                        else
                            response.setFault(new InternalErrorFault(ex)); 
                
                return response;
            }catch(Exception ex2) {
                throw new RuntimeException(ex2.toString());
            }
        }
    }
    
    private static void fillReturnAddress(Addressing request,
            Addressing response)
            throws JAXBException, SOAPException {
        response.setMessageId(UUID_SCHEME + UUID.randomUUID().toString());
        
        // messageId can be missing in a malformed request
        final String msgId = request.getMessageId();
        if (msgId != null) {
            response.addRelatesTo(msgId);
        }
        
        if (response.getBody().hasFault()) {
            final EndpointReferenceType faultTo = request.getFaultTo();
            if (faultTo != null) {
                response.setTo(faultTo.getAddress().getValue());
                response.addHeaders(faultTo.getReferenceParameters());
                return;
            }
        }
        
        final EndpointReferenceType replyTo = request.getReplyTo();
        if (replyTo != null) {
            response.setTo(replyTo.getAddress().getValue());
            response.addHeaders(replyTo.getReferenceParameters());
            return;
        }
        
        final EndpointReferenceType from = request.getFrom();
        if (from != null) {
            response.setTo(from.getAddress().getValue());
            response.addHeaders(from.getReferenceParameters());
            return;
        }
        
        response.setTo(Addressing.ANONYMOUS_ENDPOINT_URI);
    }
    
    private static Message handleResponse(final Message response,
            final long maxEnvelopeSize) throws SOAPException, JAXBException,
            IOException {
        
        if(response instanceof Identify) {
            return response;
        }
        
        if(!(response instanceof Management))
            throw new IllegalArgumentException(" Invalid internal response " +
                    "message " + response);
        
        Management mgtResp = (Management) response;
        return handleResponse(mgtResp, null, maxEnvelopeSize);
    }
    
    private static Message handleResponse(final Management response,
            final FaultException fex, final long maxEnvelopeSize) throws SOAPException, JAXBException,
            IOException {
        if (fex != null)
            response.setFault(fex);
        
        logMessage(LOG, response);
        
        if (maxEnvelopeSize < MIN_ENVELOPE_SIZE) {
			final String err = "MaxEnvelopeSize of '" + maxEnvelopeSize
					+ "' is set too small to encode faults "
					+ "(needs to be at least " + MIN_ENVELOPE_SIZE + ")";
			LOG.fine(err);
			final EncodingLimitFault fault = new EncodingLimitFault(err,
					EncodingLimitFault.Detail.MAX_ENVELOPE_SIZE);
			response.setFault(fault);
		} else {
			if (maxEnvelopeSize >= Integer.MAX_VALUE) {
				// Don't check MaxEnvelopeSize if not specified or set to maximum.
				// NOTE: The official maximum is actually Long.MAX_VALUE,
				//        but the ByteArrayOutputStream we use has a maximum
				//        size of Integer.MAX_VALUE. We therefore cannot actually
				//        check if the size exceeds Integer.MAX_VALUE.
				LOG.fine("MaxEnvelopeSize not specified or set to maxiumum value.");
			} else {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				response.writeTo(baos);
				int length = baos.size();

				if (length > maxEnvelopeSize) {
					final String err = "MaxEnvelopeSize of '"
							+ maxEnvelopeSize
							+ "' is smaller than the size of the response message: "
							+ Integer.toString(length);
					LOG.fine(err);
					final EncodingLimitFault fault = new EncodingLimitFault(
							err,
							EncodingLimitFault.Detail.MAX_ENVELOPE_SIZE_EXCEEDED);
					response.setFault(fault);
				} else {
					// Message size is OK.
					LOG.fine("Response actual size is smaller than specified MaxEnvelopeSize.");
				}
			}
		}
        
        final String dest = response.getTo();
        if (!Addressing.ANONYMOUS_ENDPOINT_URI.equals(dest)) {
            LOG.fine("Non anonymous reply to send to : " + dest);
            final int status = sendAsyncReply(response);
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Response to " + dest + " returned " + status);
            }
            return null;
        }
        
        LOG.fine("Anonymous reply to send.");
        
        return response;
    }
    
    private static int sendAsyncReply(final Management response)
    throws IOException, SOAPException, JAXBException {
        return HttpClient.sendResponse(response);
    }
    
    static void logMessage(Logger logger,
            final Message msg) throws IOException, SOAPException {
        // expensive serialization ahead, so check first
        if (logger.isLoggable(Level.FINE)) {
            if(msg == null) {
                logger.fine("Null message to log. Reply has perhaps been " +
                        "sent asynchronously");
                return;
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            final byte[] content = baos.toByteArray();
            
            final String encoding = msg.getContentType() == null ? null :
                msg.getContentType().getEncoding();
            
            logger.fine("Encoding [" + encoding + "]");
            
            if(encoding == null)
                logger.fine(new String(content));
            else
                logger.fine(new String(content, encoding));
            
        }
    }
}
