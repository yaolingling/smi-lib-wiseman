/*
 * WSEnumeration.java
 *
 * Created on October 16, 2007, 4:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.enumeration.*;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.soap.SOAPException;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

/**
 *
 * @author jfdenise
 */
public interface WSEnumerationRequest extends WSAddressingRequest {
     public Enumerate getEnumerate() throws JAXBException, SOAPException;
     public EnumerationExtensions.Mode getMode() throws JAXBException, SOAPException;
     public EnumerationModeType getModeType() throws JAXBException, SOAPException;
     public boolean getOptimize() throws JAXBException, SOAPException;
     public int getMaxElements() throws JAXBException, SOAPException;
     public Duration getMaxTime() throws JAXBException, SOAPException;
     public AttributableEmpty getRequestTotalItemsCountEstimate() throws JAXBException, SOAPException;
     public Pull getPull() throws JAXBException, SOAPException;
     public Release getRelease() throws JAXBException, SOAPException;
       /**
     * This one is needed to compute the Subscription Manager EPR.
     * It shows that Enumeration and WS-Management are coupled.
     */
    public String getResourceURIForEnumeration() throws JAXBException, SOAPException;
    public DialectableMixedDataType getWsmanEnumerationFilter() throws JAXBException, SOAPException;
    public FilterType getEnumerationFilter() throws JAXBException, SOAPException;
}
