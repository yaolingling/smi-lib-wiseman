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
 * $Id: CannotProcessFilterFault.java,v 1.2 2006-05-01 23:32:21 akhilarora Exp $
 */

package com.sun.ws.management.enumeration;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class CannotProcessFilterFault extends SenderFault {
    
    public static final QName CANNOT_PROCESS_FILTER = 
            new QName(Enumeration.NS_URI, "CannotProcessFilter", Enumeration.NS_PREFIX);
    public static final String CANNOT_PROCESS_FILTER_REASON = 
            "The requested filter could not be processed.";

    public CannotProcessFilterFault(final String explanation) {
        this(SOAP.createFaultDetail(explanation, null, null, null));
    }
    
    public CannotProcessFilterFault(final Node... details) {
        super(Enumeration.FAULT_ACTION_URI, CANNOT_PROCESS_FILTER, CANNOT_PROCESS_FILTER_REASON, details);
    }
}
