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
 * $Id: EndpointUnavailableFault.java,v 1.2 2006/05/01 23:32:20 akhilarora Exp $
 */

package com.sun.ws.management.addressing;

import com.sun.ws.management.soap.ReceiverFault;
import com.sun.ws.management.soap.SOAP;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class EndpointUnavailableFault extends ReceiverFault {
    
    public static final QName ENDPOINT_UNAVAILABLE = 
            new QName(Addressing.NS_URI, "EndpointUnavailable", Addressing.NS_PREFIX);
    public static final String ENDPOINT_UNAVAILABLE_REASON =
            "The specified endpoint is currently unavailable.";
    
    public static enum Detail {
        INVALID_RESOURCE_URI("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidResourceURI"),
        INVALID_VALUES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValues");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    public EndpointUnavailableFault(final Detail... detail) {
        this(SOAP.createFaultDetail(null, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, null));
    }
    
    public EndpointUnavailableFault(final String duration, final Detail... detail) {
        this(SOAP.createFaultDetail(null, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, Addressing.RETRY_AFTER, duration));
    }
    
    public EndpointUnavailableFault(final Node... details) {
        super(Addressing.FAULT_ACTION_URI, ENDPOINT_UNAVAILABLE, ENDPOINT_UNAVAILABLE_REASON, details);
    }
}
