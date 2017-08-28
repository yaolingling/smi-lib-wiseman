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
 * $Id: TransferSupport.java,v 1.4 2007/05/30 15:30:05 simeonpinder Exp $
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: TransferSupport.java,v $
 **Revision 1.4  2007/05/30 15:30:05  simeonpinder
 **Numerous changes-->
 **
 **ManagementUtility:
 **-added selectorSet creator method
 **-added org.w3c.dom.Node to String convenience method.
 **
 **EventingSupport:
 **-added default expiration fileds/accessors
 **
 **HttpClient:
 **-elminated some duplicate logging logic
 **-added commented out template for how to include HttpCommons' HttpClient to seamlessly handle http transport
 **
 **TransferSupport:
 **-added mechanism to include body in Management payload
 **
 **eventsubman_Handler:
 **-fixed problem with static initialization
 **-pulled create actions into own method.
 **
 **eventcreator:
 **
 **MetadataTest:
 **-modified test to use newer metadata pruning method.
 **-added method to more thoroughly exercise the metadata payload description.
 **
 **Revision 1.3  2007/05/30 13:57:30  nbeers
 **Add HP copyright header
 **
 **
 * $Id: TransferSupport.java,v 1.4 2007/05/30 15:30:05 simeonpinder Exp $
 *
 */
package com.sun.ws.management.framework.transfer;

import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.AttributedURI;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.ActionNotSupportedFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.xml.XmlBinding;

/**
 * This class provides default, overrideable behavior for
 * objects that support WS-Transfer. Custom actions can be
 * implemented by creating methids names after actions.
 * @author wire
 *
 */
public class TransferSupport implements Transferable {

	public static final org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory addressingFactory = new org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory();
    public static final ObjectFactory managementFactory = new ObjectFactory();
    public static final org.xmlsoap.schemas.ws._2004._09.transfer.ObjectFactory xferFactory = new  org.xmlsoap.schemas.ws._2004._09.transfer.ObjectFactory();
	public static final QName FRAGMENT_TRANSFER = new QName(Management.NS_URI, "FragmentTransfer", Management.NS_PREFIX);

	public TransferSupport() {
		super();
	}

	public void create(HandlerContext context,Management request, Management response) {
		throw new ActionNotSupportedFault();
	}

	public void delete(HandlerContext context,Management request, Management response) {
		throw new ActionNotSupportedFault();
	}

	public void get(HandlerContext context,Management request, Management response) {
		throw new ActionNotSupportedFault();
	}

	public void put(HandlerContext context,Management request, Management response) {
		throw new ActionNotSupportedFault();
	}


	/**
	 * Utility method used to append a create response to a SOAP body.
	 * May have to change as there is sctually no element called CreateResponse
	 * in the spec version of this response.
	 * @param response
	 * @param resourceUri
	 * @param selectors
	 * @throws JAXBException
	 */
	@SuppressWarnings("static-access")
	protected void appendCreateResponse(Management response, String resourceUri,Map<String,String> selectors) throws JAXBException  {
		EndpointReferenceType epr=null;
		epr = response.createEndpointReference(Addressing.ANONYMOUS_ENDPOINT_URI, null,null,null,null);

		JAXBElement<EndpointReferenceType> resp = xferFactory.createResourceCreated(epr);

        // Build the reference parameters
        ReferenceParametersType refParams = new ReferenceParametersType();
        epr.setReferenceParameters(refParams);
        List<Object> paramList = refParams.getAny();

        // Set our resource URI (Similar to our classname)
        AttributableURI resourceURI = new AttributableURI();
        resourceURI.setValue(resourceUri);
        paramList.add(managementFactory.createResourceURI(resourceURI));

        // Set the selectors required to find this instance again
        SelectorSetType selectorSetType = new SelectorSetType();
        List<SelectorType> selectorList = selectorSetType.getSelector();

        // Add a selector to the list
        for (String key : selectors.keySet()) {
            SelectorType nameSelector = new SelectorType();
            nameSelector.setName(key);
            nameSelector.getContent().add(selectors.get(key));
            selectorList.add(nameSelector);
		}

        paramList.add(managementFactory.createSelectorSet(selectorSetType));
       XmlBinding xmlBinding = response.getXmlBinding(); 
        Document responseDoc = Management.newDocument();
		try {
			xmlBinding.marshal(resp, responseDoc );
		} catch (JAXBException e) {
			final String explanation =
				 "XML Binding marshall failed for object of type: "
                + resp.getClass().getName();
			throw new InternalErrorFault(SOAP.createFaultDetail(explanation, null, e, null));
		}

		try {
			response.getBody().addDocument(responseDoc );
		} catch (SOAPException e) {
			throw new InternalErrorFault();
		}
	}

    protected SOAPElement locateFragmentHeader(SOAPElement[] allHeaders) {
		SOAPElement fragmentHeader = null;
		if(allHeaders!=null){
		 for (int i = 0; ((fragmentHeader==null) &&(i < allHeaders.length)); i++) {
			SOAPElement element = allHeaders[i];
			QName elems = element.getElementQName();
			if(elems!=null){
				if((elems.getLocalPart().equalsIgnoreCase(FRAGMENT_TRANSFER.getLocalPart()))&&
				   (elems.getPrefix().equalsIgnoreCase(FRAGMENT_TRANSFER.getPrefix()))&&
				   (elems.getNamespaceURI().equalsIgnoreCase(FRAGMENT_TRANSFER.getNamespaceURI()))
				   ){
				  fragmentHeader = element;
				}
			}
		 }
		}
		return fragmentHeader;
	}

	protected String extractFragmentMessage(SOAPElement element) {
		String xpathExp = "";
		//DONE: populate xpathExp
		if(element!=null){
		  NodeList elem = element.getChildNodes();
		  for (int j = 0; j < elem.getLength(); j++) {
			Node node = elem.item(j);
			xpathExp = node.getNodeValue();
		  }
		}
	   return xpathExp;
	}

	public static EndpointReferenceType createEpr(String endpointUrl,String resourceUri, Map<String, String> selectors) throws JAXBException
    {
        // Get a JAXB Epr
        EndpointReferenceType epr = addressingFactory.createEndpointReferenceType();
        AttributedURI addressURI = addressingFactory.createAttributedURI();
        addressURI.getOtherAttributes().put(SOAP.MUST_UNDERSTAND, Boolean.TRUE.toString());
        if(endpointUrl==null)
        	addressURI.setValue(Addressing.ANONYMOUS_ENDPOINT_URI);
        else
        	addressURI.setValue(endpointUrl);
        epr.setAddress(addressURI);

        // Build the reference parameters
        ReferenceParametersType refParams = new ReferenceParametersType();
        epr.setReferenceParameters(refParams);
        List<Object> paramList = refParams.getAny();

        // Set our resource URI (Similar to our classname)
        AttributableURI resourceURIType = new AttributableURI();
        resourceURIType.setValue(resourceUri);
        paramList.add(managementFactory.createResourceURI(resourceURIType));

        // Set the selectors required to find this instance again
        SelectorSetType selectorSetType = new SelectorSetType();
        List<SelectorType> selectorList = selectorSetType.getSelector();

        // Add a selector to the list
        for (String key : selectors.keySet())
        {
            SelectorType nameSelector = new SelectorType();
            nameSelector.setName(key);
            nameSelector.getContent().add(selectors.get(key));
            selectorList.add(nameSelector);
        }

        paramList.add(managementFactory.createSelectorSet(selectorSetType));

//        XmlBinding xmlBinding = new XmlBinding(null);
//        Document document = Management.newDocument();
//        xmlBinding.marshal(addressingFactory.createEndpointReference(epr), document);

        return epr;

    }
}
