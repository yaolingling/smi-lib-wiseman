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
 * $Id: InvalidRepresentationFault.java,v 1.2 2006-05-01 23:32:24 akhilarora Exp $
 */

package com.sun.ws.management.transfer;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class InvalidRepresentationFault extends SenderFault {
    
    public static final QName INVALID_REPRESENTATION = 
            new QName(Transfer.NS_URI, "InvalidRepresentation", Transfer.NS_PREFIX);
    public static final String INVALID_REPRESENTATION_REASON = 
        "The XML content was invalid.";

    public static enum Detail {
        INVALID_VALUES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValues"),
        MISSING_VALUES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MissingValues"),
        INVALID_NAMESPACE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidNamespace"),
        INVALID_FRAGMENT("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidFragment");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    public InvalidRepresentationFault(final Detail... detail) {
        this(SOAP.createFaultDetail(null, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, null));
    }
    
    public InvalidRepresentationFault(final Node... details) {
        super(Transfer.FAULT_ACTION_URI, INVALID_REPRESENTATION, INVALID_REPRESENTATION_REASON, details);
    }
}
