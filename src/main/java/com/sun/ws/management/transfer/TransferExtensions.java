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
 **$Log: not supported by cvs2svn $
 **Revision 1.15  2007/05/30 20:31:06  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: TransferExtensions.java,v 1.16 2007-10-30 09:27:47 jfdenise Exp $
 */


package com.sun.ws.management.transfer;

import com.sun.ws.management.MessageUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.xpath.XPathExpressionException;

import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.transfer.ObjectFactory;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.eventing.FilteringRequestedUnavailableFault;
import com.sun.ws.management.server.BaseSupport;
import com.sun.ws.management.server.EnumerationSupport;
import com.sun.ws.management.server.Filter;
import com.sun.ws.management.server.NamespaceMap;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.xml.XPath;

public class TransferExtensions extends Transfer {
    
    public static final QName FRAGMENT_TRANSFER =
            new QName(Management.NS_URI, "FragmentTransfer", Management.NS_PREFIX);
    
    public static final QName XML_FRAGMENT =
            new QName(Management.NS_URI, "XmlFragment", Management.NS_PREFIX);
    
    public static final QName DIALECT = new QName("Dialect");
    
    public static final ObjectFactory FACTORY = new ObjectFactory();
    
    public TransferExtensions() throws SOAPException {
        super();
    }
    
    public TransferExtensions(final Addressing addr) throws SOAPException {
        super(addr);
    }
    
    public TransferExtensions(final InputStream is) throws SOAPException, IOException {
        super(is);
    }
    
	/**
	 * Utility method to create an EPR.
	 * 
	 * @param address
	 *            The transport address of the service.
	 * 
	 * @param resource
	 *            The resource being addressed.
	 * 
	 * @param selectorMap
	 *            Selectors used to identify the resource.
	 */
	public static EndpointReferenceType createEndpointReference(
			final String address, final String resource,
			final Map<String, String> selectorMap) {
		return EnumerationSupport.createEndpointReference(address, 
				                                          resource, 
				                                          selectorMap);
	}
	
    /**
     * Returns the FragmentTransfer SOAPElement if it exists
     *
     * @return FragmentTransfer SOAPElement if it exists else null
     */
    public SOAPHeaderElement getFragmentHeader() throws SOAPException {
        for (final SOAPElement se : getChildren(getHeader(), FRAGMENT_TRANSFER)) {
            if (se instanceof SOAPHeaderElement) {
                final SOAPHeaderElement she = (SOAPHeaderElement) se;
                if (she.getMustUnderstand()) {
                    return she;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the FragmentTransfer Dialect if FragmentTrasfer header exists
     *
     * @return FragmentTransfer Dialect if FragmentTrasfer header exists else null
     */
    public String getFragmentDialect() throws SOAPException {
    	return getFragmentDialect(getFragmentHeader());
    }
    
    private static String getFragmentDialect(SOAPHeaderElement header) throws SOAPException {
    	
    	if (header == null)
           return null;
    	
    	String dialect = header.getAttributeValue(TransferExtensions.DIALECT);
    	if ((dialect == null) || (dialect.length() == 0)) {
    		dialect = XPath.NS_URI;
    	}
    	return dialect;
    }
    
    /**
     * Sets the FragmentTransfer header in the message.
     * 
	 * @param expression a filter expression to be applied against the resource.
	 *        For the {@link XPath#NS_URI XPath} dialect this should be a string
	 *        containing the XPath expression. For other dialects this must be an
	 *        object recognized by the marshaller.
	 * @param dialect the dialect to be used in filter expressions.
	 *        The {@link XPath#NS_URI XPath} dialect 
	 *        can be used for XPath expressions.
     * @throws SOAPException
     * @throws JAXBException
     */
    public void setFragmentHeader(final Object expression,
    		                      final String dialect) 
    throws SOAPException, JAXBException {
    	setFragmentHeader(expression, null, dialect);
    }
    
    /**
     * Sets the FragmentTransfer header in the message.
     * 
	 * @param expression a filter expression to be applied against the resource.
	 *        For the {@link XPath#NS_URI XPath} dialect this should be a string
	 *        containing the XPath expression. For other dialects this must be an
	 *        object recognized by the marshaller.
	 * @param namespaces prefix and namespace map for namespaces used in the filter
	 *        expression.
	 * @param dialect the dialect to be used in filter expressions.
	 *        The {@link XPath#NS_URI XPath} dialect 
	 *        can be used for XPath expressions. 
     * @throws SOAPException
     * @throws JAXBException
     */
    public void setFragmentHeader(final Object expression,
            final Map<String, String> namespaces,
            final String dialect)
            throws SOAPException, JAXBException {
        
        // remove existing, if any
        removeChildren(getHeader(), FRAGMENT_TRANSFER);

        final JAXBElement<DialectableMixedDataType> fragmentTransfer = MessageUtil.createFragmentHeader(expression, namespaces, dialect);
        if(fragmentTransfer == null)
            return;
        
        // set the SOAP Header for Fragment Transfer
        getXmlBinding().marshal(fragmentTransfer, getHeader());
        
        // Add the expression namespaces, if any, to the fragment header
        if ((namespaces != null) && (namespaces.size() > 0)) {
            final SOAPHeaderElement header = getFragmentHeader();
            final Iterator<Entry<String, String>> ni = namespaces.entrySet()
            .iterator();
            while (ni.hasNext()) {
                final Entry<String, String> entry = ni.next();
                header.addNamespaceDeclaration(entry.getKey(), entry.getValue());
            }
        }
    }
	
    /**
     * Inserts the FragmentTransferHeader into the SOAP Headers
     *
     * @param fragmentTransferHeader
     * @throws SOAPException
     */
    private void setFragmentHeader(final SOAPHeaderElement fragmentTransferHeader) 
    throws SOAPException {
        // remove existing, if any
        removeChildren(getHeader(), FRAGMENT_TRANSFER);
        getHeader().addChildElement(fragmentTransferHeader);
    }
    
    /**
     * Inserts the resource into the body of the response.
     *
     * @param resource  Resource to be returned for this Get. The binding must be
     *                  able to marshall this object.
     * @throws JAXBException
     * @throws XPathExpressionException 
     */
    public void setGetResponse(
			final Object resource) throws JAXBException, SOAPException {
    	
        // add the payload to the body
		if (resource instanceof Document) {
			getBody().addDocument((Document)resource);
		} else {
            getXmlBinding().marshal(resource, getBody());
		}
    }
    
    /**
     * Inserts the resource as a Fragment-level Get response into the body 
     * and adds the FragmentTransfer Header to the SOAP Headers.
     * Fragment-level filtering is done on the resource using 
     * filter dialect in fragmentTransferHeader, if fragmentTransferHeader
     * is not null, otherwise the compete resource is added to the response.
     *
     * @param fragmentTransferHeader The Fragement Transfer Header
     * to be returned in the headers, if any
     * @param resource  Resource to be returned for this Get. The binding must be
     *                  able to marshall this object.
     * @throws JAXBException
     * @throws XPathExpressionException 
     */
    public void setFragmentGetResponse(
			final SOAPHeaderElement fragmentTransferHeader,
			final Object resource) throws JAXBException, SOAPException {

		final Object fragment = createFragment(fragmentTransferHeader, resource);
        
        // add the header
		if (fragmentTransferHeader != null)
		    setFragmentHeader(fragmentTransferHeader);
		
        // add the payload to the body
		if (fragment instanceof Document) {
			getBody().addDocument((Document)fragment);
		} else {
            getXmlBinding().marshal(fragment, getBody());
		}
    }

	private Object createFragment(final SOAPHeaderElement fragmentTransferHeader, final Object resource) throws SOAPException {
		final Object fragment;
		if (fragmentTransferHeader != null) {
			final Filter filter = createFilter(fragmentTransferHeader);

				final Element item;

				if (resource instanceof Element) {
					item = (Element) resource;
				} else if (resource instanceof Document) {
					item = ((Document)resource).getDocumentElement();
				} else {
					final Document content = getDocumentBuilder().newDocument();
					
					try {
						getXmlBinding().marshal(resource, content);
					} catch (Exception e) {
						final String explanation = 
							 "XML Binding marshall failed for object of type: "
		                     + resource.getClass().getName();
						throw new InternalErrorFault(SOAP.createFaultDetail(explanation, null, e, null));
					}
                    item = content.getDocumentElement();
				}
				// Evaluate & create the XmlFragmentElement
				fragment = createXmlFragment(filter.evaluate(item));
				final String nsURI = item.getNamespaceURI();
				final String nsPrefix = item.getPrefix();
				if (nsPrefix != null && nsURI != null) {
					getBody().addNamespaceDeclaration(nsPrefix, nsURI);
				}
		} else {
			fragment = resource;
		}
		return fragment;
	}
    
    /**
     * Inserts the Fragment-level Get response into the body 
     * and adds the FragmentTransfer Header to the SOAP Headers.
     *
     * @param fragmentTransferHeader The Fragement Transfer Header to be returned in the headers
     * @param fragment  Fragment object for the response
     * @throws JAXBException
     */
    public void setFragmentGetResponse(final SOAPHeaderElement fragmentTransferHeader, 
            final JAXBElement<MixedDataType> fragment) throws JAXBException, SOAPException {

        setFragmentHeader(fragmentTransferHeader);
        //add payload to the body
        getXmlBinding().marshal(fragment, getBody());
    }
    
    /**
     * Handles the Fragment-level Get response by wrapping the content into
     * an XmlFragment element and adding the FragmentTransfer Header to the SOAP Headers.
     *
     * @param fragmentTransferHeader The Fragement Transfer Header to be returned in the headers
     * @param content                A Collection of Objects
     * @throws JAXBException
     */
    public void setFragmentGetResponse(final SOAPHeaderElement fragmentTransferHeader, 
            final List<Object> content) throws JAXBException, SOAPException {
        
        final JAXBElement<MixedDataType> xmlFragment = MessageUtil.buildXmlFragment(content);

        setFragmentGetResponse(fragmentTransferHeader, xmlFragment);
    }
    
    /**
     * Handles the Delete operation response
     *
     * @throws SOAPException
     */
    public void setDeleteResponse()
			throws SOAPException {
    	// Nothing to do
	}
    
    /**
     * Handles the Fragment Delete operation response
     *
     * @param fragmentHeader
     * @throws SOAPException
     */
    public void setFragmentDeleteResponse(final SOAPHeaderElement fragmentHeader)
			throws SOAPException {
		setFragmentHeader(fragmentHeader);
	}
    
    
    /**
     * Handles the Put operation response
     *
     * @throws SOAPException
     * @throws JAXBException 
     */
    public void setPutResponse(final Object content)
			throws SOAPException, JAXBException {
		// add the payload to the body if it's a fragment
		if (content instanceof Document) {
			getBody().addDocument((Document) content);
		} else {
			getXmlBinding().marshal(content, getBody());
		}
	}    
    
    /**
     * Handles the Put operation response
     *
     * @throws SOAPException
     */
    public void setPutResponse()
			throws SOAPException {
    	// Nothing to do
	}
    
    /**
     * Inserts the resource as a Fragment-level Put response into the body 
     * and adds the FragmentTransfer Header to the SOAP Headers.
     * Fragment-level filtering is done on the resource using 
     * filter dialect in fragmentTransferHeader, if fragmentTransferHeader
     * is not null, otherwise a standard put response is done.
     *
     * @param fragmentHeader the Fragement Transfer Header
     * to be returned in the headers, if any
     * @param resource  Resource to be returned for this Get. The binding must be
     *                  able to marshall this object.
     * @throws JAXBException
     * @throws XPathExpressionException 
     */
    public void setFragmentPutResponse(final SOAPHeaderElement fragmentHeader,
			final Object resource) throws SOAPException, JAXBException {

		// add the header
		if (fragmentHeader != null) {
			setFragmentHeader(fragmentHeader);

			final Object fragment = createFragment(fragmentHeader, resource);

			// add the payload to the body if it's a fragment
			if (fragment instanceof Document) {
				getBody().addDocument((Document) fragment);
			} else {
				getXmlBinding().marshal(fragment, getBody());
			}
		}
	}
    
    /**
     * Inserts the fragment content as a Fragment-level Put response into the body 
     * and adds the FragmentTransfer Header to the SOAP Headers.
     * 
     * @param fragmentHeader the Fragement Transfer Header
     * to be returned in the headers
     * @param content content to be inserted into the XmlFragment
     * @throws SOAPException
     * @throws JAXBException
     */
    public void setFragmentPutResponse(final SOAPHeaderElement fragmentHeader,
			final List<Object> content) throws SOAPException,
			JAXBException {

		final JAXBElement<MixedDataType> xmlFragment = MessageUtil.buildXmlFragment(content);

		// set the fragment transfer header
		setFragmentHeader(fragmentHeader);
		// add payload to the body
		getXmlBinding().marshal(xmlFragment, getBody());

	}
    
    /**
     * @deprecated Use {@link #setFragmentPutResponse(SOAPHeaderElement, List)}
     */
    public void setFragmentPutResponse(final SOAPHeaderElement fragmentHeader, 
                                       final List<Object> requestContent,
                                       final String expression,
                                       final List<Node> nodes) 
            throws SOAPException, JAXBException {

        final JAXBElement<MixedDataType> xmlFragment = MessageUtil.buildXmlFragment(requestContent);
        
        //set the fragment transfer header
        setFragmentHeader(fragmentHeader);
        //add payload to the body
        getXmlBinding().marshal(xmlFragment, getBody());
        
    }

    /**
     * Handles the Create operation response
     *
     * @param epr the EndpointReference of the newly created object
     * @throws SOAPException
     * @throws JAXBException 
     */
    public void setCreateResponse(final EndpointReferenceType epr)
			throws SOAPException, JAXBException {
    	JAXBElement<EndpointReferenceType> jaxbEpr = FACTORY.createResourceCreated(epr);
    	
        //add payload to the body
        getXmlBinding().marshal(jaxbEpr, getBody());
	}
    
    /**
     * Handles the Fragment Create operation response
     *
     * @param epr the EndpointReference of the newly created object
     * @throws SOAPException
     * @throws JAXBException 
     */
    public void setFragmentCreateResponse(final SOAPHeaderElement fragmentHeader,
    		                              final EndpointReferenceType epr)
			throws SOAPException, JAXBException {

		// add the fragment header if specified
		if (fragmentHeader != null) {
			setFragmentHeader(fragmentHeader);
		}
		// add payload to the body
		setCreateResponse(epr);
	}
    
    /**
     * @deprecated use {@link #setFragmentCreateResponse(SOAPHeaderElement, EndpointReferenceType)}
     * 
     * @param fragmentHeader
     * @param requestContent
     * @param expression
     * @param nodes
     * @param epr
     * @throws SOAPException
     * @throws JAXBException
     */
    public void setFragmentCreateResponse(final SOAPHeaderElement fragmentHeader, 
            final List<Object> requestContent, final String expression, final List<Node> nodes, 
            final EndpointReferenceType epr) throws SOAPException, JAXBException {
		// add the fragment header if specified
		if (fragmentHeader != null) {
			setFragmentHeader(fragmentHeader);
		}
		// add payload to the body
		setCreateResponse(epr);
        
    }

	public Object getResource(final QName element) throws JAXBException, SOAPException {
		Object resource = super.getResource(element);
		if (resource == null) {
	        resource = unbind(getBody(), XML_FRAGMENT);
		}
		return resource;
	}
	
	/**
	 * Create a Filter from an Enumeration request
	 * 
	 * @return Returns a Filter object if a filter exists in the request,
	 *         otherwise null.
	 * @throws CannotProcessFilterFault,
	 *             FilteringRequestedUnavailableFault, InternalErrorFault
	 */
	public static Filter createFilter(final Transfer request)
			throws CannotProcessFilterFault, FilteringRequestedUnavailableFault {
		try {
			final TransferExtensions transfer = new TransferExtensions(
					request);
			final SOAPHeaderElement fragmentHeader = transfer.getFragmentHeader();
			
            return createFilter(fragmentHeader);
		} catch (SOAPException e) {
			throw new InternalErrorFault(e.getMessage());
		}
	}
	
	private static Filter createFilter(final SOAPHeaderElement fragmentHeader)
			throws CannotProcessFilterFault, FilteringRequestedUnavailableFault, SOAPException {

			if (fragmentHeader == null)
				return null;

			final NamespaceMap nsMap = new NamespaceMap(fragmentHeader);
			
			NodeList nodes = fragmentHeader.getChildNodes();
			final List<Node> filter = new ArrayList<Node>(nodes.getLength());
			for (int i = 0; i < nodes.getLength(); i++) {
				filter.add(nodes.item(i));
			}
			return BaseSupport.createFilter(getFragmentDialect(fragmentHeader),
					filter, nsMap);
	}
	
    /**
     * Create a JAXBElement object that is a wsman:XmlFragment from a list
     * of XML Nodes.
     * 
     * @param nodes Nodes to be inserted into the XmlFragment element.
     * @return XmlFragment JAXBElement object.
     */
	public static JAXBElement<MixedDataType> createXmlFragment(final List<Node> nodes) {
		return BaseSupport.createXmlFragment(nodes);
	}
	
    /**
	 * Create a JAXBElement object that is a wsman:XmlFragment from a NodeList
	 * 
	 * @param nodes Nodes to be inserted into the XmlFragment element.
	 * @return XmlFragment JAXBElement object.
	 */
	public static JAXBElement<MixedDataType> createXmlFragment(final NodeList nodes) {
		return BaseSupport.createXmlFragment(nodes);
	}
}
