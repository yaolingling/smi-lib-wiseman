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
 * $Id: SchemaValidationErrorFault.java,v 1.2 2006-05-01 23:32:20 akhilarora Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class SchemaValidationErrorFault extends SenderFault {
    
    public static final QName SCHEMA_VALIDATION_ERROR = 
            new QName(Management.NS_URI, "SchemaValidationError", Management.NS_PREFIX);
    public static final String SCHEMA_VALIDATION_ERROR_REASON =
            "The supplied SOAP violates the corresponding XML Schema definition.";
    
    public SchemaValidationErrorFault(final String explanation) {
        this(SOAP.createFaultDetail(explanation, null, null, null));
    }
    
    public SchemaValidationErrorFault(final Node... details) {
        super(Management.FAULT_ACTION_URI, SCHEMA_VALIDATION_ERROR, SCHEMA_VALIDATION_ERROR_REASON, details);
    }
}
