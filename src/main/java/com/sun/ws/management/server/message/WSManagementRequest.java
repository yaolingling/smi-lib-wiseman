/*
 * WSManagementRequest.java
 *
 * Created on October 15, 2007, 3:57 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.soap.FaultException;
import java.net.URI;
import java.util.Set;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.SOAPException;
import org.dmtf.schemas.wbem.wsman._1.wsman.Locale;
import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionSet;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;

/**
 *
 *Abstraction of a WS-Management request
 */
public interface WSManagementRequest extends WSEventingRequest, WSTransferRequest, WSEnumerationRequest {
    public Duration getTimeout() throws JAXBException, SOAPException;
    public MaxEnvelopeSizeType getMaxEnvelopeSize() throws SOAPException, JAXBException;
    public boolean isIdentify() throws SOAPException, JAXBException;
    public URI getResourceUri() throws SOAPException, JAXBException, FaultException;
    public OptionSet getOptionSet() throws SOAPException, JAXBException;
    public Locale getLocale() throws SOAPException, JAXBException;
    public Set<SelectorType> getSelectors() throws Exception;
    public void validate() throws SOAPException, JAXBException, FaultException;
    public NamespaceContext getNamespaceContext() throws Exception;
    
}
