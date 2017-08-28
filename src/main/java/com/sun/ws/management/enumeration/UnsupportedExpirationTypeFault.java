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
 * $Id: UnsupportedExpirationTypeFault.java,v 1.3 2006/05/01 23:32:21 akhilarora Exp $
 */

package com.sun.ws.management.enumeration;

import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class UnsupportedExpirationTypeFault extends SenderFault {
    
    public static final QName UNSUPPORTED_EXPIRATION_TYPE = 
            new QName(Enumeration.NS_URI, "UnsupportedExpirationType", Enumeration.NS_PREFIX);
    public static final String UNSUPPORTED_EXPIRATION_TYPE_REASON = 
        "The specified expiration type is not supported.";
    
    public UnsupportedExpirationTypeFault(final Node... details) {
        super(Enumeration.FAULT_ACTION_URI, UNSUPPORTED_EXPIRATION_TYPE, 
                UNSUPPORTED_EXPIRATION_TYPE_REASON, details);
    }
}
