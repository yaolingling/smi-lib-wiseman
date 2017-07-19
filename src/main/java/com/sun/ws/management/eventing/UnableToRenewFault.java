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
 * $Id: UnableToRenewFault.java,v 1.2 2006-05-01 23:32:22 akhilarora Exp $
 */

package com.sun.ws.management.eventing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class UnableToRenewFault extends SenderFault {
    
    public static final QName UNABLE_TO_RENEW = 
            new QName(Eventing.NS_URI, "UnableToRenew", Eventing.NS_PREFIX);
    public static final String UNABLE_TO_RENEW_REASON =
            "The subscription could not be renewed.";
    
    public UnableToRenewFault(final String explanation) {
        this(SOAP.createFaultDetail(explanation, null, null, null));
    }
    
    public UnableToRenewFault(final Node... details) {
        super(Eventing.FAULT_ACTION_URI, UNABLE_TO_RENEW, UNABLE_TO_RENEW_REASON, details);
    }
}
