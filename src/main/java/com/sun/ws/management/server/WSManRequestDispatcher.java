/*
 * Copyright 2005 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: not supported by cvs2svn $
 **Revision 1.14  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSManRequestDispatcher.java,v 1.1 2007-10-30 09:28:22 jfdenise Exp $
 */

package com.sun.ws.management.server;

import com.sun.ws.management.Management;
import com.sun.ws.management.server.message.WSManagementRequest;
import com.sun.ws.management.server.message.WSManagementResponse;
import com.sun.ws.management.soap.FaultException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;

public abstract class WSManRequestDispatcher implements Callable {
    
    private static final Logger LOG = Logger.getLogger(WSManRequestDispatcher.class.getName());

    private final HandlerContext context;
    private final WSManagementRequest request;
    private final WSManagementResponse response;

    public WSManRequestDispatcher(final WSManagementRequest req, final WSManagementResponse resp, final HandlerContext ctx) 
    throws JAXBException, SOAPException {
        request = req;
        response = resp;
        context = ctx;
    }
    
    public WSManRequestDispatcher(HandlerContext ctx) {
        context = ctx;
        request = null;
        response = null;
    }
    
    public void validateRequest()
    throws SOAPException, JAXBException, FaultException {
        getRequest().validate();
    }
    
    public void authenticate() throws SecurityException, JAXBException, SOAPException {
    	// TODO: perform access control, throw SecurityException to deny access
    }
    
    private void log(final byte[] bits) throws UnsupportedEncodingException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(new String(bits, getContext().getCharEncoding()));
        }
    }

    public HandlerContext getContext() {
        return context;
    }

    public WSManagementRequest getRequest() {
        return request;
    }

    public WSManagementResponse getResponse() {
        return response;
    }
}
