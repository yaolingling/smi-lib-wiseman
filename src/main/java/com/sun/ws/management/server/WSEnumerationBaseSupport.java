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
 **Revision 1.53  2007/10/30 09:27:30  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.52  2007/10/02 10:43:43  jfdenise
 **Fix for bug ID 134, Enumeration Iterator look up is static
 **Applied to Enumeration and Eventing
 **
 **Revision 1.51  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSEnumerationBaseSupport.java,v 1.1 2007-10-31 12:25:38 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;

/**
 * A helper class that encapsulates some of the arcane logic to allow data
 * sources to be enumerated using the WS-Enumeration protocol.
 *
 * @see IteratorFactory
 * @see EnumerationIterator
 */
public class WSEnumerationBaseSupport extends BaseSupport {
    
    protected WSEnumerationBaseSupport() {
    }
    
    /**
     * Utility method to create an EPR for accessing individual elements of an
     * enumeration directly.
     *
     * @param address
     *            The transport address of the service.
     *
     * @param resource
     *            The resource being addressed.
     *
     * @param selectorMap
     *            Selectors used to identify the resource. Optional.
     */
    public static EndpointReferenceType createEndpointReference(final String address,
            final String resource,
            final Map<String, String> selectorMap) {
        
        final ReferenceParametersType refp = Addressing.FACTORY
                .createReferenceParametersType();
        
        final AttributableURI attributableURI = Management.FACTORY
                .createAttributableURI();
        attributableURI.setValue(resource);
        refp.getAny()
        .add(Management.FACTORY.createResourceURI(attributableURI));
        
        if (selectorMap != null) {
            final SelectorSetType selectorSet = Management.FACTORY
                    .createSelectorSetType();
            final Iterator<Entry<String, String>> si = selectorMap.entrySet()
            .iterator();
            while (si.hasNext()) {
                final Entry<String, String> entry = si.next();
                final SelectorType selector = Management.FACTORY
                        .createSelectorType();
                selector.setName(entry.getKey());
                selector.getContent().add(entry.getValue());
                selectorSet.getSelector().add(selector);
            }
            refp.getAny()
            .add(Management.FACTORY.createSelectorSet(selectorSet));
        }
        
        return Addressing.createEndpointReference(address, null, refp, null,
                null);
    }
}
