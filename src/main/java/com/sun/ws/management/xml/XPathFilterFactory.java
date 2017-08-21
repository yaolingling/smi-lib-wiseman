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
 **$Log: XPathFilterFactory.java,v $
 **Revision 1.3  2007/05/30 20:31:06  nbeers
 **Add HP copyright header
 **
 **
 * $Id: XPathFilterFactory.java,v 1.3 2007/05/30 20:31:06 nbeers Exp $
 */

package com.sun.ws.management.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.eventing.InvalidMessageFault;
import com.sun.ws.management.server.Filter;
import com.sun.ws.management.server.FilterFactory;
import com.sun.ws.management.server.NamespaceMap;
import com.sun.ws.management.soap.FaultException;

/**
 * XPath based Filtering factory
 */
public class XPathFilterFactory implements FilterFactory {

    /**
     * Filter creation
     * @param content The filter content. In this case an XPath expression (String)
     * located in the list first element.
     * @param namespaces An XML namespaces map.
     * @return A Filter handling XPath filtering.
     *
     * @throws com.sun.ws.management.soap.FaultException If any WS-MAN related protocol exception occurs.
     * @throws java.lang.Exception If any other exception occurs.
     */
    public Filter newFilter(List content, NamespaceMap namespaces) throws FaultException, Exception {
        return new XPathEnumerationFilter(content, namespaces);
    }

    class XPathEnumerationFilter implements Filter {
        private final String expression;
        private final XPath xpath = com.sun.ws.management.xml.XPath.XPATH_FACTORY.newXPath();
        private final XPathExpression filter;
        private final NamespaceMap initialNamespaceMap;
        private final Map<String, String> aggregateNamespaces = new HashMap<String, String>();
        private NamespaceMap aggregateNamespaceMap = null;

        /** Creates a new instance of XPathEnumerationFilter */
        public XPathEnumerationFilter(List filterExpressions, NamespaceMap namespaces)
        throws FaultException, Exception {
            if (filterExpressions == null) {
				throw new InvalidMessageFault("Missing a filter expression");
			}
            final Object expr = filterExpressions.get(0);
            if (expr == null) {
                throw new InvalidMessageFault("Missing filter expression");
            }
            if (expr instanceof String) {
                expression = (String) expr;
            } else if (expr instanceof Node) {
                expression = ((Node)expr).getTextContent();
            } else {
                throw new InvalidMessageFault("Invalid filter expression type: " +
                        expr.getClass().getName());
            }

            initialNamespaceMap = namespaces;
            if (initialNamespaceMap != null) {
                aggregateNamespaces.putAll(initialNamespaceMap.getMap());
                aggregateNamespaceMap = new NamespaceMap(aggregateNamespaces);
                xpath.setNamespaceContext(aggregateNamespaceMap);
            }

            // compile the expression just to see if it's valid
            try {
                filter = xpath.compile(expression);
            } catch(XPathExpressionException ex) {
                throw new Exception("Unable to compile XPath expression : "
                        + expression);
            }
        }

        public NodeList evaluate(final Node content) throws CannotProcessFilterFault {

            try {
				return (NodeList) filter.evaluate(content, XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				throw new CannotProcessFilterFault(CannotProcessFilterFault.CANNOT_PROCESS_FILTER_REASON);
			}
        }

		public String getDialect() {
			return com.sun.ws.management.xml.XPath.NS_URI;
		}

		public Object getExpression() {
			return expression;
		}
    }
}
