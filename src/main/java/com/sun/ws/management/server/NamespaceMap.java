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
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: not supported by cvs2svn $
 **
 * $Id: NamespaceMap.java,v 1.7 2007-05-30 20:31:04 nbeers Exp $
 */

package com.sun.ws.management.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.SOAPEnvelope;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.ws.management.Message;

public final class NamespaceMap implements NamespaceContext {
    
    private final Map<String, String> namespaces = new HashMap<String, String>();
    
    // key:prefix value:URI
    public NamespaceMap(final Map<String, String>... nsMaps) {
        // combine declarations from supplied maps, if any
        if (nsMaps != null) {
            for (final Map<String, String> ns : nsMaps) {
                namespaces.putAll(ns);
            }
        }
    }
    
    public NamespaceMap(final NamespaceMap... nsMaps) {
        // combine declarations from supplied maps, if any
        if (nsMaps != null) {
            for (final NamespaceMap ns : nsMaps) {
                namespaces.putAll(ns.namespaces);
            }
        }
    }
    
    // walk the document tree from this point to the top of the document
    // to extract all namespace declarations active with this node,
    // combining namespaces from (optional) supplied maps
    public NamespaceMap(final Node node, final NamespaceMap... nsMaps) {
        this(nsMaps);
        scanNodeRecursive(node);
    }
    
    // walk the message tree to extract all namespace declarations,
    // combining namespaces from (optional) supplied maps
    public NamespaceMap(final Message msg, final NamespaceMap... nsMaps) {
        this(msg.getEnvelope(), nsMaps);

        // collect all namespaces declared in the soap envelope
        final SOAPEnvelope env = msg.getEnvelope();
        final Iterator<String> pi = env.getNamespacePrefixes();
        while (pi.hasNext()) {
            final String prefix = pi.next();
            final String uri = env.getNamespaceURI(prefix);
            assert uri != null : "namespace uri for env prefix " + prefix + " cannot be null";
            namespaces.put(prefix, uri);
        }
    }
    
    private void scanNodeRecursive(final Node node) {
        if (node == null) {
            return;
        }
        
        final String prefix = node.getPrefix();
        final String uri = node.getNamespaceURI();
        
        switch (node.getNodeType()) {
            case Node.ATTRIBUTE_NODE:
                if ("xmlns".equals(prefix) && "http://www.w3.org/2000/xmlns/".equals(uri)) {
                    // Only add it if it does not already exist
                	if (namespaces.get(node.getLocalName()) == null)
                        namespaces.put(node.getLocalName(), node.getNodeValue());
                }
                break;
                
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
            case Node.ELEMENT_NODE:
                if (prefix != null) {
                	// Only add it if it does not already exist
                	if (namespaces.get(prefix) == null)
                        namespaces.put(prefix, uri);
                }
                
                final NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    for (int i = attributes.getLength(); i >= 0; i--) {
                        scanNodeRecursive(attributes.item(i));
                    }
                }
                
                final Node parent = node.getParentNode();
                scanNodeRecursive(parent);
                break;
        }
    }
    
    public Iterator getPrefixes(final String namespaceURI) {
        final Set<String> prefixes = new HashSet<String>();
        final Iterator<String> pi = namespaces.keySet().iterator();
        while (pi.hasNext()) {
            final String prefix = pi.next();
            final String uri = namespaces.get(prefix);
            if (uri != null) {
                if (uri.equals(namespaceURI)) {
                    prefixes.add(prefix);
                }
            }
        }
        return prefixes.iterator();
    }
    
    public String getPrefix(final String namespaceURI) {
        final Iterator<String> pi = namespaces.keySet().iterator();
        while (pi.hasNext()) {
            final String prefix = pi.next();
            final String uri = namespaces.get(prefix);
            if (uri != null) {
                if (uri.equals(namespaceURI)) {
                    return prefix;
                }
            }
        }
        return null;
    }
    
    public String getNamespaceURI(final String prefix) {
        return namespaces.get(prefix);
    }
    
    // key:prefix value:URI
    public Map<String, String> getMap() {
        return Collections.unmodifiableMap(namespaces);
    }
}
