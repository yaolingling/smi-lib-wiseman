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
 * $Id: Identify.java,v 1.3 2006/07/10 01:41:07 akhilarora Exp $
 */

package com.sun.ws.management.identify;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.soap.SOAP;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import org.w3c.dom.Node;

public class Identify extends SOAP {
    
    public static final String NS_PREFIX = "id";
    public static final String NS_URI = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd";
    
    public static final QName IDENTIFY = new QName(NS_URI, "Identify", NS_PREFIX);
    public static final QName IDENTIFY_RESPONSE = new QName(NS_URI, "IdentifyResponse", NS_PREFIX);
    
    public static final QName PROTOCOL_VERSION = new QName(NS_URI, "ProtocolVersion", NS_PREFIX);
    public static final QName PRODUCT_VENDOR = new QName(NS_URI, "ProductVendor", NS_PREFIX);
    public static final QName PRODUCT_VERSION = new QName(NS_URI, "ProductVersion", NS_PREFIX);
    
    // extra info returned in an IdentifyResponse
    public static final QName BUILD_ID = new QName(NS_URI, "BuildId", NS_PREFIX);
    public static final QName SPEC_VERSION = new QName(NS_URI, "SpecVersion", NS_PREFIX);
    
    public Identify() throws SOAPException {
        super();
    }
    
    public Identify(final Addressing addr) throws SOAPException {
        super(addr);
    }
    
    public Identify(final InputStream is) throws SOAPException, IOException {
        super(is);
    }
    
    public void setIdentify() throws SOAPException {
        getEnvelope().addNamespaceDeclaration(NS_PREFIX, NS_URI);
        getBody().addBodyElement(IDENTIFY);
    }
    
    public void setIdentifyResponse(final String vendor, final String productVersion,
            final String protocolVersion, final Map<QName, String> more) throws SOAPException {
        
        getEnvelope().addNamespaceDeclaration(NS_PREFIX, NS_URI);
        
        final SOAPBodyElement response = getBody().addBodyElement(IDENTIFY_RESPONSE);
        response.addChildElement(PRODUCT_VENDOR).setTextContent(vendor);
        response.addChildElement(PRODUCT_VERSION).setTextContent(productVersion);
        response.addChildElement(PROTOCOL_VERSION).setTextContent(protocolVersion);
        
        if (more != null) {
            final Iterator<Entry<QName, String> > mi = more.entrySet().iterator();
            while (mi.hasNext()) {
                final Entry<QName, String> entry = mi.next();
                response.addChildElement(entry.getKey()).setTextContent(entry.getValue());
            }
        }
    }
    
    public SOAPElement getIdentify() throws SOAPException {
        final SOAPElement[] ide = getChildren(getBody(), IDENTIFY);
        if (ide.length == 0) {
            return null;
        }
        return ide[0];
    }
    
    public SOAPElement getIdentifyResponse() throws SOAPException {
        final SOAPElement[] idr = getChildren(getBody(), IDENTIFY_RESPONSE);
        if (idr.length == 0) {
            return null;
        }
        return idr[0];
    }
}
