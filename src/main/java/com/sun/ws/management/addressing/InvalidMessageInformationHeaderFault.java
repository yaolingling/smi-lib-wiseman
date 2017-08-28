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
 * $Id: InvalidMessageInformationHeaderFault.java,v 1.2 2006/05/01 23:32:20 akhilarora Exp $
 */

package com.sun.ws.management.addressing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class InvalidMessageInformationHeaderFault extends SenderFault {
    
    public static final QName INVALID_MESSAGE_INFORMATION_HEADER = 
            new QName(Addressing.NS_URI, "InvalidMessageInformationHeader", Addressing.NS_PREFIX);
    public static final String INVALID_MESSAGE_INFORMATION_HEADER_REASON =
            "A message information header is not valid and the message cannot be processed.";
    
    public InvalidMessageInformationHeaderFault(final String invalidHeader) {
        this(SOAP.createFaultDetail(null, null, null, null, invalidHeader));
    }
    
    public InvalidMessageInformationHeaderFault(final Node... details) {
        super(Addressing.FAULT_ACTION_URI, INVALID_MESSAGE_INFORMATION_HEADER,
                INVALID_MESSAGE_INFORMATION_HEADER_REASON, details);
    }
}
