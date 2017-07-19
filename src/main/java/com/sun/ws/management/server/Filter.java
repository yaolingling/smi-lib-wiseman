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
 * $Id: Filter.java,v 1.3 2007-05-30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.ws.management.soap.FaultException;

/**
 * Filtering support interface
 *
 */
public interface Filter {
    /**
     * Filter evaluation of a Node. The filter expression
     * is applied against the Node. If the Filter supports
     * projections, i.e. <code>SELECT Lastname, Age FROM User</code>,
     * the List returned will contain the selected Nodes, otherwise
     * the entire Node that matches the filter portion of the
     * expression, e.g.
     * <code>WHERE Age = '30'</code>, will be returned.
     *
     * @param content A node to apply filter on.
     * @throws FaultException In case filtering encounters an error in processing.
     * @return a List of Nodes that match the filter. If there
     * is no match an empty List is returned.
     *
     */
    public NodeList evaluate(final Node content)
    throws FaultException;

    /**
     * This method returns the dialect associated with this filter, e.g.
     * the XPath 1.0 dialect URI.
     *
     * @return The dialect associated with this filter.
     */
    public String getDialect();

    /**
     * This method returns an Object with the expression for this filter.
     * The object returned is Filter specific. For the default XPath 1.0 filter
     * the object type will be String.
     *
     * @return the expression object.
     */
    public Object getExpression();
}
