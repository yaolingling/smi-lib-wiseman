/*
 * Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt 
 **
 **$Log: Metadata.java,v $
 **Revision 1.3  2007/05/30 20:30:30  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: Metadata.java,v 1.3 2007/05/30 20:30:30 nbeers Exp $
 */
package com.sun.ws.management.mex;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.xmlsoap.schemas.ws._2004._09.enumeration.ObjectFactory;

import com.sun.ws.management.addressing.Addressing;

public class Metadata extends Addressing{

	    public static final String INITIALIZE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Initialize";
	    public static final String INITIALIZE_RESPONSE_URI = 
	    	"http://schemas.xmlsoap.org/ws/2004/09/transfer/InitializeResponse";
	
	    public static final String NS_PREFIX = "mex";
	    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2004/09/mex";
	    
	    public static final String METADATA_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Request";
	    public static final String METADATA_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Response";
	    
	    public static final ObjectFactory FACTORY = new ObjectFactory();
	    
	    public Metadata() throws SOAPException {
	        super();
	    }
	    
	    public Metadata(final Addressing addr) throws SOAPException {
	        super(addr);
	    }
	    
	    public Metadata(final InputStream is) throws SOAPException, IOException {
	        super(is);
	    }
}
