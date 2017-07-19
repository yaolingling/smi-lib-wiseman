/*
 * WSEventingRequest.java
 *
 * Created on October 16, 2007, 11:36 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.xmlsoap.schemas.ws._2004._08.eventing.FilterType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Renew;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;

/**
 *
 * @author jfdenise
 */
public interface WSEventingRequest extends WSAddressingRequest {
    public Renew getRenew() throws JAXBException, SOAPException ;
    public String getIdentifier() throws JAXBException, SOAPException;
    public Subscribe getSubscribe() throws JAXBException, SOAPException;
    /**
     * This one is needed to compute the Subscription Manager EPR.
     * It shows that Eventing and WS-Management are coupled.
     */
    public String getResourceURIForEventing() throws JAXBException, SOAPException;
    public Unsubscribe getUnsubscribe() throws JAXBException, SOAPException;
    public DialectableMixedDataType getWsmanEventingFilter() throws JAXBException, SOAPException;
    public FilterType getEventingFilter() throws JAXBException, SOAPException;
}
