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
 * $Id: EventSourceUnableToProcessFault.java,v 1.2 2006-05-01 23:32:22 akhilarora Exp $
 */

package com.sun.ws.management.eventing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class EventSourceUnableToProcessFault extends SenderFault {
    
    public static final QName EVENT_SOURCE_UNABLE_TO_PROCESS = 
            new QName(Eventing.NS_URI, "EventSourceUnableToProcess", Eventing.NS_PREFIX);
    public static final String EVENT_SOURCE_UNABLE_TO_PROCESS_REASON =
            "The event source cannot process the subscription.";
    
    public static enum Detail {
        UNUSABLE_ADDRESS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/UnusableAddress");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    public EventSourceUnableToProcessFault(final String explanation, final Detail... detail) {
        this(SOAP.createFaultDetail(explanation, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, null));
    }
    
    public EventSourceUnableToProcessFault(final Node... details) {
        super(Eventing.FAULT_ACTION_URI, EVENT_SOURCE_UNABLE_TO_PROCESS,
                EVENT_SOURCE_UNABLE_TO_PROCESS_REASON, details);
    }
}
