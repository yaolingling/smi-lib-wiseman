/*
 * Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt 
 **
 **$Log: not supported by cvs2svn $
 **Revision 1.1  2007/10/30 09:28:22  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.3  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: WSEventingIteratorFactory.java,v 1.2 2008-06-04 08:06:30 denis_rachal Exp $
 */
package com.sun.ws.management.server;

import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.server.message.WSEventingRequest;
import com.sun.ws.management.soap.FaultException;

public interface WSEventingIteratorFactory {
    /**
     * EnumerationIterator creation.
     * 
     * @param context the HandlerContext
     * @param request the Enumeration request that this iterator is to fufill
     * 
     * @throws com.sun.ws.management.soap.FaultException If a WS-MAN protocol related exception occurs.
     * @return An enumeration iterator for the request
     */
    public EnumerationIterator newIterator(final HandlerContext context, 
			final WSEventingRequest request)
    throws UnsupportedFeatureFault, FaultException;
}
