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
 * $Id: EncodingLimitFault.java,v 1.2 2006-05-01 23:32:18 akhilarora Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class EncodingLimitFault extends SenderFault {
    
    public static final QName ENCODING_LIMIT = 
            new QName(Management.NS_URI, "EncodingLimit", Management.NS_PREFIX);
    public static final String ENCODING_LIMIT_REASON =
            "An internal encoding limit was exceeded in a request or would be violated if the message were processed.";
    
    public static enum Detail {
        URI_LIMIT_EXCEEDED("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/URILimitExceeded"),
        MAX_ENVELOPE_SIZE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxEnvelopeSize"),
        MAX_ENVELOPE_SIZE_EXCEEDED("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxEnvelopeSizeExceeded"),
        SERVICE_ENVELOPE_LIMIT("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/ServiceEnvelopeLimit"),
        SELECTOR_LIMIT("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/SelectorLimit"),
        OPTION_LIMIT("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/OptionLimit"),
        CHARACTER_SET("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/CharacterSet"),
        UNREPORTABLE_SUCCESS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/UnreportableSuccess"),
        WHITESPACE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/Whitespace"),
        ENCODING_TYPE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/EncodingType");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    
    public EncodingLimitFault(final String explanation, final Detail... detail) {
        this(SOAP.createFaultDetail(explanation, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, null));
    }
    
    public EncodingLimitFault(final Node... details) {
        super(Management.FAULT_ACTION_URI, ENCODING_LIMIT, ENCODING_LIMIT_REASON, details);
    }
}
