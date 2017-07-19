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
 * $Id: HandlerContext.java,v 1.4 2007-09-18 13:06:56 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.security.Principal;
import java.util.Map;


public interface HandlerContext {
    public static final String JAX_WS_CONTEXT = "com.sun.management.transport.jaxws.context";
    public static final String SERVLET_CONTEXT = "com.sun.ws.management.transport.servlet.context";
	public static final String SERVLET_REQUEST_ATTRIBUTES = "com.sun.ws.management.transport.servlet.request.attributes";
    public static final String MBEAN_SERVER = "com.sun.ws.management.mbeanserver";
    public static final String SUBJECT = "com.sun.ws.management.subject";
    public Map<String,?> getRequestProperties();
    public String getURL();
    public String getCharEncoding();
    public String getContentType();
    public Principal getPrincipal();
}
