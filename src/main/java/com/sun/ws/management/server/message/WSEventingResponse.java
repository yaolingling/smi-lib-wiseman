/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 * 
 ** Copyright (C) 2006-2008 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Denis Rachal (denis.rachal@hp.com)
 **
 */
/*
 * WSEventingResponse.java
 *
 * Created on October 16, 2007, 11:39 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.ws.management.server.message;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

/**
 *
 * @author jfdenise
 */
public interface WSEventingResponse extends SOAPResponse{
    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires)
    		throws SOAPException, JAXBException;
    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires,
            final Object... extensions)
            throws SOAPException, JAXBException;
    public void setRenewResponse(final String expires)
            throws SOAPException, JAXBException;
    public void setRenewResponse(final String expires, final Object... extensions)
            throws SOAPException, JAXBException;
    public void setSubscriptionManagerEpr(EndpointReferenceType mgr)
    		throws JAXBException, SOAPException;
}
