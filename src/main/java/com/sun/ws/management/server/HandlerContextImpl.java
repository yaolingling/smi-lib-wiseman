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
 * $Id: HandlerContextImpl.java,v 1.4 2007/04/06 09:57:13 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.security.Principal;
import java.util.Map;

public final class HandlerContextImpl implements HandlerContext {
    private String charEncoding;
    private String contentType;
    private Principal principal;
    private Map<String, ?> requestProperties;
    private String url;
    public HandlerContextImpl(final Principal principal, final String contentType, 
            final String charEncoding, final String url, final Map<String, ?> requestProperties) {
        this.principal = principal;
        this.contentType = contentType;
        this.charEncoding = charEncoding;
        this.requestProperties = requestProperties;
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getCharEncoding() {
        return charEncoding;
    }

    public String getURL() {
        return url;
    }

    public Map<String, ?> getRequestProperties() {
        return requestProperties;
    }
}
