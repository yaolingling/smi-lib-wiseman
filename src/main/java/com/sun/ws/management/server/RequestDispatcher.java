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
 **
 * $Id: RequestDispatcher.java,v 1.14 2007-05-30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import com.sun.ws.management.EncodingLimitFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.Management;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transport.ContentType;
import com.sun.ws.management.transport.HttpClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

public abstract class RequestDispatcher implements Callable {
    
    private static final Logger LOG = Logger.getLogger(RequestDispatcher.class.getName());
    private static final String UUID_SCHEME = "uuid:";

    protected final HandlerContext context;
    protected final Management request;
    protected final Management response;
    
    public RequestDispatcher(final Management req, final HandlerContext ctx) 
    throws JAXBException, SOAPException {
        request = req;
        context = ctx;
        response = new Management();
        response.setXmlBinding(req.getXmlBinding());

        final ContentType contentType = 
                ContentType.createFromHttpContentType(
                context.getContentType());
        response.setContentType(contentType);
    }
    
    public void validateRequest()
    throws SOAPException, JAXBException, FaultException {
        request.validate();
    }
    
    public void authenticate() throws SecurityException, JAXBException, SOAPException {
    	
    	final Principal user = context.getPrincipal();
    	final String resource = request.getResourceURI();
    	// TODO: perform access control, throw SecurityException to deny access
    }
    
    private void log(final byte[] bits) throws UnsupportedEncodingException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(new String(bits, context.getCharEncoding()));
        }
    }
}
