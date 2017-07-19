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
 * $Id: WSManJAXWSEndpoint.java,v 1.1 2007-04-06 09:57:51 jfdenise Exp $
 */

package com.sun.ws.management.server.jaxws;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.Message;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgent;
import com.sun.ws.management.transport.ContentType;
import com.sun.ws.management.xml.XmlBinding;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.ws.WebServiceContext;
import java.lang.management.ManagementFactory;
import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import org.xml.sax.SAXException;

/**
 * JAX-WS Compliant endpoint.
 */
@WebServiceProvider()
@ServiceMode(value=Service.Mode.MESSAGE)
public abstract class WSManJAXWSEndpoint implements Provider<SOAPMessage> {
    
    private static final Logger LOG = Logger.getLogger(WSManJAXWSEndpoint.class.getName());
    private WSManAgent agent;
    
    /**
     * JAX-WS Endpoint constructor
     */
    public WSManJAXWSEndpoint() {
    }
    
    public SOAPMessage invoke(SOAPMessage message) {
        Management request = null;
        HandlerContext ctx = null;
        
        try {
            request = new Management(message);
            request.setXmlBinding(getAgent().getXmlBinding());
            Principal principal = getWebServiceContext().getUserPrincipal();
            String contentType = ContentType.DEFAULT_CONTENT_TYPE.getMimeType();
            String encoding = request.getContentType() == null ? null : request.getContentType().getEncoding();
            String url = request.getTo();
            Object servletContext = getWebServiceContext().getMessageContext().get(MessageContext.SERVLET_CONTEXT);
            WebServiceContext webctx = getWebServiceContext();
            Map<String, Object> props = new HashMap<String, Object>(1);
            props.put(HandlerContext.SERVLET_CONTEXT, servletContext);
            props.put(HandlerContext.JAX_WS_CONTEXT, webctx);
            
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Context properties : " + " contentType " + contentType + 
                        ", encoding " + encoding + ", url " + url + ", servletContext"
                        + servletContext + ", webctx " + 
                        webctx);

            ctx = new HandlerContextImpl(principal, contentType, encoding, url, props);
            Message reply = getAgent().handleRequest(request, ctx);
            
            // reply being null means that no reply is to be sent back. 
            // The reply has been handled asynchronously
            if(reply == null)
                return null;
            
            return reply.getMessage();
            
        }catch(Exception ex) {
            try {
                Management response = new Management();
                response.setXmlBinding(getAgent().getXmlBinding());
                response.setFault(new InternalErrorFault(ex.getMessage()));
                return response.getMessage();
            }catch(Exception ex2) {
                // We can't handle the internal error.
                throw new RuntimeException(ex2.getMessage());
            }
        }
    }
    
    private synchronized WSManAgent getAgent() throws SAXException {
        if(agent == null)
            agent = createWSManAgent();
        return agent;
    }
    
    /*
     * In case this class is extended, Annotations @WebServiceProvider()
     * @ServiceMode(value=Service.Mode.MESSAGE) @Resource must be set on the extended class
     * or JAX-WS will not recognize the endpoint as being a valid JAX-WS endpoint.
     * This method is used to retrieve the injected resource of the extended class.
     */
    protected abstract WebServiceContext getWebServiceContext();
    
    protected abstract WSManAgent createWSManAgent() throws SAXException;
}
