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
 **Revision 1.7  2007/10/02 10:27:19  jfdenise
 **Removed useless invalid import that make wiseman not compilable on Java 6
 **
 **Revision 1.6  2007/09/06 16:19:07  simeonpinder
 **Issue 131:
 **modified to allow for removal of Enumeration/Expires requests.  Setting the EnumerationMessageValues.setExpires() to a value < 0 will result in no Expires node being generated.
 **
 **Revision 1.5  2007/06/19 19:50:39  nbeers
 **Set the DefaultTimeout header in addition to the maxElement header for enumeration pulls
 **
 **Revision 1.4  2007/06/18 17:57:11  nbeers
 **Fix for Issue #119 (EnumerationUtility.buildMessage() generates incorrect msg).
 **
 **Revision 1.3  2007/05/30 20:31:03  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: EnumerationUtility.java,v 1.8 2007-11-30 14:32:39 denis_rachal Exp $
 */
package com.sun.ws.management.enumeration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.AnyListType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.xml.XmlBinding;

/** This class is meant to provide general utility functionality for
 *  Enumeration message instances and all of their related extensions.
 *  To enable messages ready for submission, Management instances are 
 *  returned from some of the methods below. 
 * 
 *  The following default values are defined when newer values have not 
 *  been supplied:
 *  	UID_SCHEME = uuid:
 *  	DEFAULT_TIMEOUT = 30000
 *      ACTION = Enumeration.ENUMERATE_ACTION_URI 
 *  	FILTER_TYPE.DIALECT = XPATH
 *  	REPLY_TO = Anonymous URI
 *  
 * @author Simeon Pinder
 */
public class EnumerationUtility {

	public static Enumeration buildMessage(Enumeration existingEnum,
			EnumerationMessageValues settings) throws SOAPException,
			JAXBException, DatatypeConfigurationException {

		final DatatypeFactory factory = DatatypeFactory.newInstance();
		
		// Set the Management values.
		final Management mgmt = ManagementUtility.buildMessage(existingEnum, settings);
		existingEnum = new Enumeration(mgmt);

		if (settings == null) {//grab a default instance if 
			settings = EnumerationMessageValues.newInstance();
		}

		//Process the EnumerationConstants instance passed in.
		//Processing ACTION for the message
		if ((settings.getEnumerationMessageActionType() != null)
				&& (settings.getEnumerationMessageActionType().trim().length() > 0)) {
			existingEnum.setAction(settings.getEnumerationMessageActionType());
		} else {
			existingEnum
					.setAction(EnumerationMessageValues.DEFAULT_ENUMERATION_MESSAGE_ACTION_TYPE);
		}

		//Processing ReplyTo
		if ((settings.getReplyTo() != null)
				&& settings.getReplyTo().trim().length() > 0) {
			existingEnum.setReplyTo(settings.getReplyTo());
		} else {
			existingEnum.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
		}

		//Processing MessageId component
		if ((settings.getUidScheme() != null)
				&& (settings.getUidScheme().trim().length() > 0)) {
			existingEnum.setMessageId(settings.getUidScheme()
					+ UUID.randomUUID().toString());
		} else {
			existingEnum
					.setMessageId(EnumerationMessageValues.DEFAULT_UID_SCHEME
							+ UUID.randomUUID().toString());
		}

		//processing Timeout/Duration and Filter
		//processing the EnumContext
		if (settings.getEnumerationContext() != null) {
			//Process for PULL action
			if (existingEnum.getAction().equals(Enumeration.PULL_ACTION_URI)) {
				EnumerationExtensions enx = new EnumerationExtensions(
						existingEnum);
				enx.setPull(settings.getEnumerationContext(), //wsen:EnumerationContext
						settings.getMaxCharacters(), // wsen:MaxCharacters
						settings.getMaxElements(), // wsen:MaxElements
						EnumerationMessageValues.newDuration(settings
								.getMaxTime()), // wsen:MaxTime
						settings.isRequestForTotalItemsCount()); // wsman:RequestTotalItemsCountEstimate
				existingEnum = new Enumeration(enx);
			} else if (existingEnum.getAction().equals(
					Enumeration.ENUMERATE_ACTION_URI)) {
				EnumerationExtensions enx = new EnumerationExtensions(
						existingEnum);
				EndpointReferenceType endTo = null;
				String expires = null;
				if (settings.getEndTo() != null
						&& settings.getEndTo().length() > 0) {
					endTo = Addressing.createEndpointReference(settings
							.getEndTo(), null, null, null, null);
				}
				if (settings.getExpires() > 0) {
					expires = factory.newDuration(settings.getExpires())
							.toString();
				}
				enx.setEnumerate(endTo, // wse:EndTo
						settings.isRequestForTotalItemsCount(), // wsman:RequestTotalItemsCountEstimate
						settings.isRequestForOptimizedEnumeration(), // wsman:OptimizeEnumeration
						settings.getMaxElements(), // wsen:MaxElements
						expires, // wsen:Expires
						EnumerationMessageValues.newFilter(
								settings.getFilter(), settings
										.getFilterDialect()), settings
								.getEnumerationMode());
				existingEnum = new Enumeration(enx);
			} else if (existingEnum.getAction().equals(
					Enumeration.RELEASE_ACTION_URI)) {
				existingEnum.setRelease(settings.getEnumerationContext());

			}
		}

		//Add namespace elements if defined.
		if ((settings.getNamespaceMap() != null)
				&& !(settings.getNamespaceMap().isEmpty())) {
			existingEnum.addNamespaceDeclarations(settings.getNamespaceMap());
		}

		//process the custom packages list that needs to be added to Binding
		if ((settings.getCustomXmlBindingPackageList() != null)
				&& (settings.getCustomXmlBindingPackageList().length > 0)) {
			String existingCustPackageList = System
					.getProperty(XmlBinding.class.getPackage().getName()
							+ ".custom.packagenames");
			String custBindList = "";
			String[] bindings = settings.getCustomXmlBindingPackageList();
			for (int i = 0; i < bindings.length; i++) {
				String pack = bindings[i];
				if (existingCustPackageList != null) {
					if (existingCustPackageList.indexOf(pack) == -1) {
						if (custBindList.trim().length() == 0) {
							custBindList = pack;
						} else {
							custBindList = "," + pack;
						}
					}
				}
			}

			// Set the system property to always create bindings with our package
			System.setProperty(XmlBinding.class.getPackage().getName()
					+ ".custom.packagenames", custBindList);

		}
		return existingEnum;
	}

	public static List<EnumerationItem> extractEnumeratedValues(
			Addressing responseMessage) throws SOAPException, JAXBException {

		//Attempt to extract the items from the request.
		final EnumerationExtensions enx = new EnumerationExtensions(responseMessage);
		final List<EnumerationItem> items = enx.getItems();

		if (items == null)
			return new ArrayList<EnumerationItem>();
		else
		    return items;
	}

	//TODO: THIS SHOULD BE exposed by a CLIENT SIDE CLASS,RESPONSE.
	/**Method attempts to extract each Item, from a pullResponse object
	 * as an array of Elements.  For Items listed in duplicate(ObjectAndEpr), then 
	 * this method attempts to locate each element and an associated EPR as well. 
	 * An EPR is an open ended element and the Element[] array list may not be 
	 * easy to evaluate. Use this method at your own discretion.  
	 * @param pullResponse encapsulates the Enumeration response received.
	 * @throws SOAPException
	 * @throws JAXBException
	 */
	public static Element[] extractItemsAsElementArray(
			final Enumeration pullResponse) throws SOAPException, JAXBException {
		if (pullResponse == null) {
			throw new IllegalArgumentException(
					"Enumeration instance cannot be null.");
		}
		Element[] elementList = null;
		Vector<Element> bag = new Vector<Element>();
		EnumerateResponse er = pullResponse.getEnumerateResponse();
		PullResponse pr = pullResponse.getPullResponse();
		if (((er == null) & (pr == null))) {
			String msg = "Unable to find an EnumeratResponse/PullResponse element in the message.";
			throw new IllegalArgumentException(msg);
		}

		List<Object> allAnys = null;
		if (pr != null) {//Process a stereotypical PullResponse object
			ItemListType itemList = pr.getItems();
			allAnys = itemList.getAny();
			for (Iterator<Object> iter = allAnys.iterator(); iter.hasNext();) {
				Object element = iter.next();
				Element elem = (Element) element;
				bag.add(elem);
			}
		} else {//Else process the stereotypical EnumerateResponse object
			allAnys = er.getAny();

			for (Iterator<Object> iter = allAnys.iterator(); iter.hasNext();) {
				Object element = iter.next();
				AnyListType lst1 = null;
				JAXBElement<AnyListType> itemsList = (JAXBElement<AnyListType>) element;

				if (itemsList.getValue() instanceof AnyListType) {
					lst1 = itemsList.getValue();
					List<Object> elements = lst1.getAny();
					for (Object obj : elements) {
						if (obj instanceof Element) {//Is a simple Element
							Element el = (Element) obj;
							bag.add(el);
						} else if (obj instanceof JAXBElement) {
							JAXBElement gen = (JAXBElement) obj;
							EndpointReferenceType eprT = (EndpointReferenceType) gen
									.getValue();
							if (eprT != null) {

								Document eprDocument = Management.newDocument();
								JAXBElement<EndpointReferenceType> eprElement = new org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory()
										.createEndpointReference(eprT);
								//binding.marshal(eprElement, eprDocument);
								pullResponse.getXmlBinding().marshal(
										eprElement, eprDocument);
								bag.add(eprDocument.getDocumentElement());
							}

						}
					}
				}
			}//End of processing for EnumerateResponse
		}
		if (bag.size() > 0) {
			elementList = new Element[bag.size()];
			bag.toArray(elementList);
		} else {
			elementList = new Element[0];
		}
		return elementList;
	}
}
