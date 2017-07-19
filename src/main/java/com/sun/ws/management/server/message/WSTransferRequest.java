/*
 * WSTransferRequest.java
 *
 * Created on October 16, 2007, 1:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.NamespaceContext;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;

/**
 *
 * @author jfdenise
 */
public interface WSTransferRequest extends WSAddressingRequest {
    public DialectableMixedDataType getFragmentHeaderContent() throws Exception;
}
