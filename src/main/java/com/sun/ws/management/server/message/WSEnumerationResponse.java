/*
 * WSEnumerationResponse.java
 *
 * Created on October 16, 2007, 4:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.enumeration.*;
import com.sun.ws.management.server.EnumerationItem;
import java.math.BigInteger;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;

/**
 *
 * @author jfdenise
 */
public interface WSEnumerationResponse extends SOAPResponse {
    public void setTotalItemsCountEstimate(final BigInteger itemCount) throws JAXBException;
    public void setEnumerateResponse(final Object context, final String expires, final Object... anys)
    throws JAXBException, SOAPException;
    public void setEnumerateResponse(final Object context, final String expires,
            final List<EnumerationItem> items, final EnumerationModeType mode, final boolean haveMore)
            throws JAXBException, SOAPException;
    public void setPullResponse(final List<EnumerationItem> items, final Object context, final boolean haveMore, EnumerationModeType mode)
    throws JAXBException, SOAPException;
}