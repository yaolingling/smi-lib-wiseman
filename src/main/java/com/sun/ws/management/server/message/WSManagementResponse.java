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
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

/**
 *
 *Abstraction of a WS-Management response
 */
public interface WSManagementResponse extends WSEventingResponse, WSTransferResponse, WSEnumerationResponse {
 
    public void setIdentifyResponse(String vendor, String productVersion,
    String protocolVersion, Map<QName, String> more) throws Exception;
    public void setAction(String actionURI) throws Exception;
    
}
