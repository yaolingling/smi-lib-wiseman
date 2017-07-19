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
 * $Id: ReceiverFault.java,v 1.4 2006-05-01 23:32:23 akhilarora Exp $
 */

package com.sun.ws.management.soap;

import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class ReceiverFault extends FaultException {
    
    public ReceiverFault(final String action, final QName subcode,
            final String reason, final Node... details) {
        super(action, SOAP.RECEIVER, subcode, reason, details);
    }
}
