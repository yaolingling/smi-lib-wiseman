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
 * $Id: Catalog.java,v 1.1 2007/04/06 10:03:09 jfdenise Exp $
 */

package com.sun.ws.management.catalog;

import com.sun.ws.management.addressing.Addressing;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.soap.SOAPException;

public class Catalog extends Addressing {
    
    public static final String NS_PREFIX = "wsmancat";
    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2005/06/wsmancat";

    public Catalog() throws SOAPException {
        super();
    }
        
    public Catalog(final Addressing addr) throws SOAPException {
        super(addr);
    }
    
    public Catalog(final InputStream is) throws SOAPException, IOException {
        super(is);
    }
}
