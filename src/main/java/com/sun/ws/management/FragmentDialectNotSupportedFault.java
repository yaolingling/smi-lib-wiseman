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
 * $Id: FragmentDialectNotSupportedFault.java,v 1.2 2006-05-24 00:31:25 akhilarora Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.soap.ReceiverFault;
import com.sun.ws.management.soap.SOAP;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class FragmentDialectNotSupportedFault extends ReceiverFault {
    
    public static final QName FRAGMENT_DIALECT_NOT_SUPPORTED = 
            new QName(Management.NS_URI, "FragmentDialectNotSupported", Management.NS_PREFIX);
    public static final String FRAGMENT_DIALECT_NOT_SUPPORTED_REASON =
            "The requested fragment filtering dialect or language is not supported.";
    
    // TODO: delete this constructor and use wsman:CannotProcessFilter fault instead when it's available
    public FragmentDialectNotSupportedFault(final String syntaxErrorMessage) {
        this(SOAP.createFaultDetail(syntaxErrorMessage, null, null, null));
    }
    
    public FragmentDialectNotSupportedFault(final String[] supportedDialects) {
        this(SOAP.createFaultDetail(null, null, null, null, (Object[]) supportedDialects));
    }
    
    public FragmentDialectNotSupportedFault(final Node... details) {
        super(Management.FAULT_ACTION_URI, FRAGMENT_DIALECT_NOT_SUPPORTED, FRAGMENT_DIALECT_NOT_SUPPORTED_REASON, details);
    }
}
