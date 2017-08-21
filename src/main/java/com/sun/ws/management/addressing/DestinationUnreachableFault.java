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
 * $Id: DestinationUnreachableFault.java,v 1.2 2006/05/01 23:32:20 akhilarora Exp $
 */

package com.sun.ws.management.addressing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class DestinationUnreachableFault extends SenderFault {
    
    public static final QName DESTINATION_UNREACHABLE = 
            new QName(Addressing.NS_URI, "DestinationUnreachable", Addressing.NS_PREFIX);
    public static final String DESTINATION_UNREACHABLE_REASON =
            "No route can be determined to reach the destination role defined by the WS-Addressing To.";

    public static enum Detail {
        INVALID_RESOURCE_URI("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidResourceURI");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    public DestinationUnreachableFault(final String explanation, final Detail... detail) {
        this(SOAP.createFaultDetail(explanation, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, null));
    }
    
    public DestinationUnreachableFault(final Node... details) {
        super(Addressing.FAULT_ACTION_URI, DESTINATION_UNREACHABLE, DESTINATION_UNREACHABLE_REASON, details);
    }
}
