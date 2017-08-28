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
 * $Id: FilterFactory.java,v 1.1 2006/12/05 10:34:43 jfdenise Exp $
 */

package com.sun.ws.management.server;

import com.sun.ws.management.soap.FaultException;
import java.util.List;


/**
 * Factory for Filters.
 */
public interface FilterFactory {
    /**
     * Filter creation.
     * @param content The Filter content.
     * @param namespaces Map of namespaces
     * @throws com.sun.ws.management.soap.FaultException If a WS-MAN protocol related exception occurs.
     * @throws java.lang.Exception If any other exception occurs.
     * @return A filter that will be used for evaluation.
     */
    public Filter newFilter(List content, NamespaceMap namespaces)
    throws FaultException, Exception;
}
