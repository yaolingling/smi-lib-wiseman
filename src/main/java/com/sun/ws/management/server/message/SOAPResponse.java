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
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 *
 * @author jfdenise
 */
public interface SOAPResponse {
    public void addNamespaceDeclarations(Map<String, String> declarations) throws SOAPException;
    
    public void setFault(FaultException f) throws Exception;
    public boolean containsFault() throws Exception;
    
    public JAXBContext getJAXBContext();
    public void addExtraHeaders(List<Object> obj, JAXBContext ctx) throws Exception;
    public void setPayload(Object jaxb, JAXBContext ctx) throws Exception;
        /**
     * Never call this method, except you have a good reason (to compute the NS Context);
     */
    public SOAPMessage toSOAPMessage() throws Exception;
}
