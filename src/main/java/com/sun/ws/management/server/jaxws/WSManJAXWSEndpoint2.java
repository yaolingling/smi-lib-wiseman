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
 * $Id: WSManJAXWSEndpoint2.java,v 1.4 2008-05-16 12:51:54 jfdenise Exp $
 */

package com.sun.ws.management.server.jaxws;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.addressing.InvalidMessageInformationHeaderFault;
import com.sun.ws.management.addressing.MessageInformationHeaderRequiredFault;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgentSupport;
import com.sun.ws.management.server.message.JAXWSMessageRequest;
import com.sun.ws.management.server.message.JAXWSMessageResponse;
import com.sun.ws.management.soap.FaultException;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.developer.JAXWSProperties;
import com.sun.xml.ws.developer.MemberSubmissionAddressing;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;
import org.xml.sax.SAXException;

/**
 * JAX-WS SUN RI Compliant endpoint.
 */
@WebServiceProvider()
@ServiceMode(value=Service.Mode.MESSAGE)
@BindingType(value=SOAPBinding.SOAP12HTTP_BINDING)
@MemberSubmissionAddressing(required=false)
public abstract class WSManJAXWSEndpoint2 implements Provider<Message> {
    
    private static final Logger LOG = Logger.getLogger(WSManJAXWSEndpoint2.class.getName());
    private WSManAgentSupport agent;
    private static Method msgWSAFaultFactory; 
    static {
        try {
            msgWSAFaultFactory = Messages.class.getMethod("createAddressingFaultMessage", WSBinding.class, QName.class);
        }catch(Exception x) {
            // OK, not running on JAX-WS 2.1.4+
        }
    }
    /**
     * JAX-WS Endpoint constructor
     */
    public WSManJAXWSEndpoint2() {
    }
    
    public Message invoke(Message message) {
        HandlerContext ctx = null;
        try {
            // Special case where no WS-A header present.
            if ((message.getHeaders().getAction(AddressingVersion.MEMBER,
                    SOAPVersion.SOAP_12) == null) && 
                    !Identify.NS_URI.equals(message.getPayloadNamespaceURI())) {
                // We should get rid-off this case, one day.
                if(msgWSAFaultFactory == null)
                    throw new MessageInformationHeaderRequiredFault(Addressing.ACTION);

                WSEndpoint endpoint = (WSEndpoint) getWebServiceContext().
                        getMessageContext().get(JAXWSProperties.WSENDPOINT);
                return (Message) msgWSAFaultFactory.invoke(null, 
                        endpoint.getBinding(), 
                        endpoint.getBinding().getAddressingVersion().actionTag);
            }
            
            Principal principal = getWebServiceContext().getUserPrincipal();
            
            Object servletContext = getWebServiceContext().getMessageContext().get(MessageContext.SERVLET_CONTEXT);
            WebServiceContext webctx = getWebServiceContext();
            Map<String, Object> props = new HashMap<String, Object>(1);
            props.put(HandlerContext.SERVLET_CONTEXT, servletContext);
            props.put(HandlerContext.JAX_WS_CONTEXT, webctx);
            JAXBContext jaxbCtx = getAgent().getJAXBContext();
            JAXWSMessageRequest request = new JAXWSMessageRequest(message, getAgent().getXmlBinding());
            JAXWSMessageResponse response = new JAXWSMessageResponse((JAXBRIContext)jaxbCtx);
            ctx = new HandlerContextImpl(principal, null, null, null, props);
            getAgent().handleRequest(request, response, ctx);
            
            return response.buildMessage();
        }catch(Exception ex) {
            //ex.printStackTrace();
            try {
                JAXWSMessageResponse response = new JAXWSMessageResponse((JAXBRIContext)getAgent().getJAXBContext());
                if(ex instanceof FaultException)
                    response.setFault((FaultException) ex);
                else
                    if(ex instanceof JAXBException)
                            response.setFault(new 
                                    InvalidMessageInformationHeaderFault(ex.
                                    toString()));
                    else
                        response.setFault(new InternalErrorFault(ex.getMessage()));
                return response.buildMessage();
            }catch(Exception ex2) {
                // Is supposed to be handled by JAX-WS
                throw new RuntimeException(ex.getMessage());
            }
        }
    }
    
    private synchronized WSManAgentSupport getAgent() throws SAXException {
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
    
    protected abstract WSManAgentSupport createWSManAgent() throws SAXException;
}
