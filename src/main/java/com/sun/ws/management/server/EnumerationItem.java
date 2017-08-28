/*
 * Copyright 2006 Sun Microsystems, Inc.
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
 **$Log: EnumerationItem.java,v $
 **Revision 1.4  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EnumerationItem.java,v 1.4 2007/05/30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

/**
 * EnumerationItem instances contain all of the necessary contents
 * from a handler for the server to return elements for an enumeation request.
 */
public final class EnumerationItem {

    /**
     * Holds an enumeration item.
     */
    private final Object item;

    /**
     * Holds a reference to the item.
     */
    private final EndpointReferenceType endpointReference;

    /**
     * Constructor.
     *
     * @param item an enumeration item that satisfies the request.
     *             The parameter type must be recognized by the Marshaller
     *             in order to allow marshalling & demarshalling this object.
     *             @see com.sun.ws.management.xml.XmlBinding
     * @param epr reference to the specified item.
     */
    public EnumerationItem(final Object item, final EndpointReferenceType epr) {
        this.item = item;
        this.endpointReference = epr;
    }
    /**
     * Getter for enumeration item.
     * @return Value of item element.
     */
    public Object getItem() {
        return item;
    }

    /**
     * Getter for item's EndpointReference.
     * @return Value of item endpointReference.
     */
    public EndpointReferenceType getEndpointReference() {
        return endpointReference;
    }
}
