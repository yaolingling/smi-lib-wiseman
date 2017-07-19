/*
 * SOAPRequest.java
 *
 * Created on October 16, 2007, 5:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.soap.*;
import com.sun.xml.ws.api.message.Header;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.SOAPMessage;

/**
 *
 * @author jfdenise
 */
public interface SOAPRequest {
    public Object getPayload(Unmarshaller u) throws Exception;
    public List<Header> getSOAPHeaders() throws Exception;
    /**
     * According to the WS-* request, returns the required NamespaceContext.
     * Root node varies according to the kind of request.
     */
   // public NamespaceContext getNamespaceContext() throws Exception;
    
    /**
     * Never call this method, except you have a good reason (to compute the NS Context);
     */
    public SOAPMessage toSOAPMessage() throws Exception;
}