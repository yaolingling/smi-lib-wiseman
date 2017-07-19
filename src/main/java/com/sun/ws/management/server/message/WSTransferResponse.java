/*
 * WSTransferResponse.java
 *
 * Created on October 16, 2007, 2:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.ws.EndpointReference;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;

/**
 *
 * @author jfdenise
 */
public interface WSTransferResponse extends WSAddressingResponse  {
     public void setFragmentResponse(DialectableMixedDataType header, 
            List<Object> values, JAXBContext context) throws Exception;
     public void setCreateResponse(EndpointReference epr) throws Exception;
     public void setFragmentCreateResponse(DialectableMixedDataType header, 
             EndpointReference epr) throws Exception;
}
