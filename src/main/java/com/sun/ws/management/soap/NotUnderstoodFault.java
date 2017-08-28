/*
 * Copyright 2006 Sun Microsystems, Inc.
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
 * $Id: NotUnderstoodFault.java,v 1.3 2006/05/01 23:32:23 akhilarora Exp $
 */

package com.sun.ws.management.soap;

import com.sun.ws.management.addressing.Addressing;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NotUnderstoodFault extends FaultException {
    
    public static final QName NOT_UNDERSTOOD = 
            new QName(SOAP.NS_URI, "NotUnderstood", SOAP.NS_PREFIX);
    public static final String NOT_UNDERSTOOD_REASON = 
            "Header not understood";
    
    private final QName headerNotUnderstood;
    
    public NotUnderstoodFault(final QName headerNotUnderstood) {
        super(Addressing.FAULT_ACTION_URI, SOAP.MUST_UNDERSTAND, null, NOT_UNDERSTOOD_REASON, (Node[]) null);

        if (headerNotUnderstood == null) {
            throw new IllegalArgumentException("Must specify which header is not understood");
        }
        this.headerNotUnderstood = headerNotUnderstood;
    }
    
    public QName getHeaderNotUnderstood() {
        return headerNotUnderstood;
    }
    
    public void encode(final SOAPEnvelope env) throws SOAPException {
        final SOAPHeader hdr = env.getHeader();
        final Document doc = env.getOwnerDocument();
        // see section 11.3 NotUnderstood Faults 
        final Element nu = doc.createElementNS(NOT_UNDERSTOOD.getNamespaceURI(), 
                NOT_UNDERSTOOD.getPrefix() + SOAP.COLON + NOT_UNDERSTOOD.getLocalPart());
        nu.setAttributeNS(SOAP.NS_URI, "qname", 
                headerNotUnderstood.getPrefix() + SOAP.COLON + headerNotUnderstood.getLocalPart());
        nu.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ns", 
                headerNotUnderstood.getNamespaceURI());
        hdr.appendChild(nu);
    }
}
