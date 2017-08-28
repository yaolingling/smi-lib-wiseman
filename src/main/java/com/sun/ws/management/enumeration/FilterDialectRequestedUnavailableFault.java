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
 * $Id: FilterDialectRequestedUnavailableFault.java,v 1.2 2006/05/01 23:32:21 akhilarora Exp $
 */

package com.sun.ws.management.enumeration;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class FilterDialectRequestedUnavailableFault extends SenderFault {
    
    public static final QName FILTER_DIALECT_REQUESTED_UNAVAILABLE =
            new QName(Enumeration.NS_URI, "FilterDialectRequestedUnavailable", Enumeration.NS_PREFIX);
    public static final String FILTER_DIALECT_REQUESTED_UNAVAILABLE_REASON =
            "The requested filtering dialect is not supported.";
    
    public FilterDialectRequestedUnavailableFault(final String[] supportedDialects) {
        this(SOAP.createFaultDetail(null, null, null,
                Enumeration.SUPPORTED_DIALECT, (Object[]) supportedDialects));
    }
    
    public FilterDialectRequestedUnavailableFault(final Node... details) {
        super(Enumeration.FAULT_ACTION_URI, FILTER_DIALECT_REQUESTED_UNAVAILABLE,
                FILTER_DIALECT_REQUESTED_UNAVAILABLE_REASON, details);
    }
}
