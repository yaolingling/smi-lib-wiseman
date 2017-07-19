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
 * $Id: XPath.java,v 1.9 2006-12-05 10:35:24 jfdenise Exp $
 */

package com.sun.ws.management.xml;

import com.sun.ws.management.server.NamespaceMap;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XPath {
    
    public static final String NS_PREFIX = "xpath";
    public static final String NS_URI = "http://www.w3.org/TR/1999/REC-xpath-19991116";
    
    public static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    
    /**
     * Filter a set of nodes based on an XPath expression.
     */
    public static List<Node> filter(final Node content,
            final String expression,
            final NamespaceMap... namespaces)
            throws XPathExpressionException {
        
        final javax.xml.xpath.XPath xpath = XPATH_FACTORY.newXPath();
        if (namespaces != null && namespaces.length > 0) {
            xpath.setNamespaceContext(namespaces[0]);
        }
        
        final XPathExpression filter = xpath.compile(expression);
        final NodeList result = (NodeList) filter.evaluate(content, XPathConstants.NODESET);
        
        final int size = result.getLength();
        final List<Node> ret = new ArrayList<Node>(size);
        for (int i = 0; i < size; i++) {
            ret.add(result.item(i));
        }
        return ret;
    }
}
