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
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: MessageInformationHeaderRequiredFault.java,v $
 **Revision 1.4  2007/05/30 20:31:06  nbeers
 **Add HP copyright header
 **
 **
 * $Id: MessageInformationHeaderRequiredFault.java,v 1.4 2007/05/30 20:31:06 nbeers Exp $
 */

package com.sun.ws.management.addressing;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class MessageInformationHeaderRequiredFault extends SenderFault {

    public static final QName MESSAGE_INFORMATION_HEADER_REQUIRED =
            new QName(Addressing.NS_URI, "MessageInformationHeaderRequired", Addressing.NS_PREFIX);
    public static final String MESSAGE_INFORMATION_HEADER_REQUIRED_REASON =
            "A required header was missing.";

    public MessageInformationHeaderRequiredFault(final QName missingHeaderName) {
        this(SOAP.createFaultDetail(null, null, null, null,
                missingHeaderName.getPrefix() + SOAP.COLON + missingHeaderName.getLocalPart()));
    }

    public MessageInformationHeaderRequiredFault(final String text,
    		final String faultDetail, final Throwable throwable,
    		final QName missingHeaderName,
    		final Object[] details) {
        this(SOAP.createFaultDetail(text, faultDetail, null, missingHeaderName,
//        		missingHeaderName.getPrefix() + SOAP.COLON + missingHeaderName.getLocalPart()));
                details));
    }

    public MessageInformationHeaderRequiredFault(final Node... details) {
        super(Addressing.FAULT_ACTION_URI, MESSAGE_INFORMATION_HEADER_REQUIRED,
                MESSAGE_INFORMATION_HEADER_REQUIRED_REASON, details);
    }
}
