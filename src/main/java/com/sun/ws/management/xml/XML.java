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
 * $Id: XML.java,v 1.2 2006-03-03 20:51:14 akhilarora Exp $
 */

package com.sun.ws.management.xml;

import java.util.Locale;
import javax.xml.namespace.QName;

public final class XML {
    
    public static final String NS_PREFIX = "xml";
    public static final String NS_URI = "http://www.w3.org/XML/1998/namespace";
    
    public static final QName LANG = new QName(NS_URI, "lang", NS_PREFIX);
    
    // Locale.toString() uses an underscore between Language and Country, XML uses a hyphen
    public static final String DEFAULT_LANG = 
            Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();

}
