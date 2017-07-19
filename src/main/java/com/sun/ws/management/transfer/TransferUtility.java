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
 **Revision 1.6  2007/05/30 20:31:06  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: TransferUtility.java,v 1.7 2007-11-30 14:32:37 denis_rachal Exp $
 */
package com.sun.ws.management.transfer;

import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.xml.XPath;

/**
 * This class is meant to provide general utility functionality for Transfer
 * instances and all of their related extensions. Management instances populated
 * with data may be returned.
 * 
 * @author Simeon
 */
public class TransferUtility {

	/**
	 * Method populates a transfer instance with the values passed in. The
	 * instance returned is NOT the response from the server based upon the
	 * values passed in.
	 * 
	 * @param destination
	 * @param resourceUri
	 * @param action
	 * @param selectors
	 * @param contents
	 * @param timeout
	 * @param uidScheme
	 * @return the message created
	 * @throws SOAPException
	 * @throws JAXBException
	 * @throws DatatypeConfigurationException
	 */
	public static Management createMessage(String destination,
			String resourceUri, String action, Set<SelectorType> selectors,
			Document contents, long timeout, String uidScheme)
			throws SOAPException, JAXBException, DatatypeConfigurationException {

		// Create return element reference
		Management mgmt = null;

		// Build a transfer instance
		Transfer xf = new Transfer();
		
		// set the action
		if ((action != null) && (action.trim().length() > 0)) {
			xf.setAction(action.trim());
		} else {
			xf.setAction(Transfer.GET_ACTION_URI);
		}
		
		// set wsa:ReplyTo to anonymous
		xf.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
		
		// use the uidScheme in message id creation
		if ((uidScheme != null) && (uidScheme.trim().length() > 0)) {
			xf.setMessageId(uidScheme + UUID.randomUUID().toString());
		} else {// use the default
			xf.setMessageId(ManagementUtility.getUidScheme()
					+ UUID.randomUUID().toString());
		}

		// Build the Management instance
		mgmt = new Management(xf);
		// populate binding
		mgmt.setXmlBinding(xf.getXmlBinding());
		// populate credentials
		mgmt.setTo(destination);
		mgmt.setResourceURI(resourceUri);

		// timeout creation
		Duration timeoutDur = null;
		if (timeout >= ManagementUtility.getDefaultTimeout()) {
			timeoutDur = DatatypeFactory.newInstance().newDuration(timeout);
			mgmt.setTimeout(timeoutDur);
		} else {// populate with the default
			timeoutDur = DatatypeFactory.newInstance().newDuration(timeout);
			mgmt.setTimeout(timeoutDur);
		}
		// process the selectors passed in.
		if ((selectors != null) && (selectors.size() > 0)) {
			mgmt.setSelectors(selectors);
		}
		// insert the contents passed in.
		if (contents != null) {
			mgmt.getBody().addDocument(contents);
		}

		return mgmt;
	}

	public static Transfer buildMessage(Transfer existingMessage,
			TransferMessageValues settings)

	throws SOAPException, JAXBException, DatatypeConfigurationException {

		// Set the Management values.
		final Management mgmt = ManagementUtility.buildMessage(existingMessage,
				settings);
		existingMessage = new Transfer(mgmt);

		if (settings == null) {// grab a default instance if
			settings = TransferMessageValues.newInstance();
		}

		// Set the wsa:Action
		if ((settings.getTransferMessageActionType() != null)
				&& (settings.getTransferMessageActionType().trim().length() > 0)) {
			existingMessage.setAction(settings.getTransferMessageActionType().trim());
		} else {
			existingMessage.setAction(Transfer.GET_ACTION_URI);
		}

		if (settings.getNamespaceMap() != null
				&& settings.getNamespaceMap().size() > 0) {
			existingMessage
					.addNamespaceDeclarations(settings.getNamespaceMap());
		}

		// Set the wsman:FragmentTransfer header
		if ((settings.getFragment() != null)
				&& (!settings.getFragment().trim().equals(""))) {
			// XPath expression
			final String expression = settings.getFragment().trim();
			TransferExtensions trnx = new TransferExtensions(existingMessage);
			// process the fragment portion
			if ((settings.getFragmentDialect() != null)
					&& (!settings.getFragmentDialect().trim().equals(""))) {
				trnx.setFragmentHeader(expression, settings
						.getFragmentDialect().trim());
			} else {
				trnx.setFragmentHeader(expression, XPath.NS_URI);
			}
			existingMessage = new Transfer(trnx);
		}

		return existingMessage;
	}
}
