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
 * $Id: ActionNotSupportedFault.java,v 1.2 2006-05-01 23:32:20 akhilarora Exp $
 */

package com.sun.ws.management.addressing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class ActionNotSupportedFault extends SenderFault {
    
    public static final QName ACTION_NOT_SUPPORTED = 
            new QName(Addressing.NS_URI, "ActionNotSupported", Addressing.NS_PREFIX);
    public static final String ACTION_NOT_SUPPORTED_REASON =
            "The action is not supported by the service.";
    
    public static enum Detail {
        ACTION_MISMATCH_DETAIL("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/ActionMismatch");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }
    
    public ActionNotSupportedFault(final String action, final Detail... detail) {
        this(SOAP.createFaultDetail(null, 
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()), 
                null, Addressing.ACTION, action));
    }
    
    public ActionNotSupportedFault(final Node... details) {
        super(Addressing.FAULT_ACTION_URI, ACTION_NOT_SUPPORTED, ACTION_NOT_SUPPORTED_REASON, details);
    }
}
