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
 **$Log: not supported by cvs2svn $
 **
 * $Id: Transfer.java,v 1.6 2007-05-30 20:31:06 nbeers Exp $
 */

package com.sun.ws.management.transfer;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import com.sun.ws.management.addressing.Addressing;

public class Transfer extends Addressing {

    public static final String NS_PREFIX = "wxf";
    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer";

    public static final String GET_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get";
    public static final String GET_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse";

    public static final String PUT_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put";
    public static final String PUT_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse";

    public static final String DELETE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete";
    public static final String DELETE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse";

    public static final String CREATE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create";
    public static final String CREATE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse";

    public static final String FAULT_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/fault";

    public Transfer() throws SOAPException {
        super();
    }

    public Transfer(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public Transfer(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public Object getResource(QName element) throws JAXBException, SOAPException {
        return unbind(getBody(), element);
    }

    public Object getResource() throws JAXBException, SOAPException {
        final SOAPElement[] elements = getChildren(getBody());
        if (elements.length == 0) {
            return null;
        }
        if (elements[0] == null) {
            return null;
        }
        return getXmlBinding().unmarshal(elements[0]);
    }
}
