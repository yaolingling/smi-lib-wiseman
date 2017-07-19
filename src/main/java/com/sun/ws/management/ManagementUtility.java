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
 **Revision 1.13  2007/06/19 12:29:33  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.12  2007/06/04 06:25:13  denis_rachal
 **The following fixes have been made:
 **
 **   * Moved test source to se/test/src
 **   * Moved test handlers to /src/test/src
 **   * Updated logging calls in HttpClient & Servlet
 **   * Fxed compiler warning in AnnotationProcessor
 **   * Added logging files for client junit tests
 **   * Added changes to support Maven builds
 **   * Added JAX-WS libraries to CVS ignore
 **
 **Revision 1.11  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 **
 *
 * $Id: ManagementUtility.java,v 1.14 2007-11-30 14:32:37 denis_rachal Exp $
 */
package com.sun.ws.management;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.AttributedURI;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transport.HttpClient;
import com.sun.ws.management.xml.XmlBinding;

/**
 * This class is meant to provide general utility functionality for Management
 * instances and all of their related extensions.
 * 
 * @author Simeon
 */
public class ManagementUtility {

	// These values are final and static so that they can be uniformly used by
	// many classes
	private static final Logger LOG = Logger.getLogger(ManagementUtility.class
			.getName());
	private static final org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory addressing_factory = new org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory();
	private static final String uidScheme = "uuid:";
	private static long defaultTimeout = 30000;
	private static Management defautInst = null;
	private static XmlBinding binding = null;
	static {
		try {
			defautInst = new Management();
			binding = defautInst.getXmlBinding();
		} catch (Exception ex) {
			// eat exception and move on.
		}
	}

	/**
	 * Takes an existing SelectorSetType container and a Map<String,String>
	 * where Key,Value or Name,Value have been supplied are accepted as
	 * parameters. A SelectorSetType instance including the Map values provided
	 * are returned.
	 * 
	 * @return SelectorSetType instance.
	 */
	public static SelectorSetType populateSelectorSetType(
			Map<String, String> selectors, SelectorSetType selectorContainer) {
		if (selectorContainer == null) {
			selectorContainer = new SelectorSetType();
		}
		// Now populate the selectorSetType
		List<SelectorType> selectorList = selectorContainer.getSelector();

		// Add a selector to the list
		for (String key : selectors.keySet()) {
			SelectorType nameSelector = new SelectorType();
			nameSelector.setName(key);
			nameSelector.getContent().add(selectors.get(key));
			selectorList.add(nameSelector);
		}
		return selectorContainer;
	}

	/**
	 * Takes a Map<String,String> of selector values and returns a container
	 * Set<SelectorType> which has the selectors passed in. Ex. Map<String,String>
	 * selectors = new HashMap<String,String>();
	 * selectors.put("firstname","Get"); selectors.put("lastname","Guy");
	 * 
	 * is wrapped in the appropriate type that will look like
	 * 
	 * <code>
	 *  &lt;wsman:SelectorSet&gt;
	 * 	 &lt;wsman:Selector Name="firstname"&gt;Get&lt;/wsman:Selector&gt;
	 *    &lt;wsman:Selector Name="lastname"&gt;Guy&lt;/wsman:Selector&gt;
	 *  &lt;/wsman:SelectorSet&gt;
	 *  </code>
	 * 
	 * @param selectorsAsProperties
	 * @return set of selectors
	 */
	public static Set<SelectorType> createSelectorType(
			Map<String, String> selectorsAsProperties) {
		Set<SelectorType> selectors = null;
		if ((selectorsAsProperties != null)
				&& (selectorsAsProperties.size() > 0)) {
			selectors = new HashSet<SelectorType>();
			Set<String> keyList = selectorsAsProperties.keySet();
			for (String key : keyList) {
				SelectorType selector = new SelectorType();
				selector.setName(key);
				selector.getContent().add(selectorsAsProperties.get(key));
				selectors.add(selector);
			}
		}
		return selectors;
	}

	/**
	 * The method takes a SelectorSetType instance and returns the Selectors
	 * defined in a Map&lt;String,String&gt; instance, with Key,Value being the
	 * values respectively.
	 * 
	 * @param selectorContainer
	 * @return Map&lt;String, String&gt; being Selector values
	 */
	public static Map<String, String> extractSelectorsAsMap(
			SelectorSetType selectorContainer) {
		// Create the Map instance to be returned
		Map<String, String> map = new HashMap<String, String>();
		List<SelectorType> selectorsList = null;

		// populate the Map with the selectorContainer contents
		if (selectorContainer != null) {
			selectorsList = selectorContainer.getSelector();
			map = extractSelectorsAsMap(map, selectorsList);
		}

		return map;
	}

	/**
	 * The method takes a List&lt;SelectorType&gt; instance and returns the
	 * Selectors defined in a Map&lt;String,String&gt; instance, with Key,Value
	 * being the values respectively.
	 * 
	 * @param map
	 * @param selectorsList
	 */
	public static Map<String, String> extractSelectorsAsMap(
			Map<String, String> map, List<SelectorType> selectorsList) {
		if (map == null) {
			map = new HashMap<String, String>();
		}
		if (selectorsList != null) {
			for (Iterator<SelectorType> iter = selectorsList.iterator(); iter
					.hasNext();) {
				SelectorType element = iter.next();
				if ((element.getName() != null)
						&& (element.getContent() != null)
						&& (((String) element.getContent().get(0))).trim()
								.length() > 0) {
					map.put(element.getName(), (String) element.getContent()
							.get(0));
				}
			}
		}
		return map;
	}

	/**
	 * Parses the header list to locate the SOAPElement identified by the QName
	 * passed in.
	 * 
	 * @param headers
	 * @param qualifiedName
	 * @return SOAPElement of the located header
	 */
	public static SOAPElement locateHeader(SOAPElement[] headers,
			QName qualifiedName) {
		SOAPElement located = null;
		if ((headers == null) || (qualifiedName == null)) {
			return located;
		} else {
			for (int i = 0; i < headers.length; i++) {
				SOAPElement header = headers[i];
				if (qualifiedName.getLocalPart().equals(
						header.getElementQName().getLocalPart())
						&& qualifiedName.getNamespaceURI().equals(
								header.getElementQName().getNamespaceURI())) {
					return header;
				}
			}
		}
		return located;
	}

	/**
	 * Attempts to build a message from the addressing instance passed in and
	 * with the ManagementMessageValues passed in. Only if the values has not
	 * already been set in the Addressing instance will the values from the
	 * constants be used.
	 * 
	 * @param instance
	 * @param settings
	 * @return message
	 * @throws SOAPException
	 * @throws JAXBException
	 * @throws DatatypeConfigurationException
	 */
	public static Management buildMessage(Addressing instance,
			ManagementMessageValues settings) throws SOAPException,
			JAXBException, DatatypeConfigurationException {
		// return reference
		Management message = null;
		// initialize if not already
		if (instance == null) {
			message = new Management();
		} else {// else use Addressing instance passed in.
			message = new Management(instance);
		}
		// initialize if not already
		if (settings == null) {
			settings = new ManagementMessageValues();
		}
		// Now process the settings values passed in.
		// Processing the To value
		if ((message.getTo() == null) || (message.getTo().trim().length() == 0)) {
			// if defaults set then use them otherwise don't
			if ((settings.getTo() != null)
					&& (settings.getTo().trim().length() > 0)) {
				message.setTo(settings.getTo());
			}
		}

		// Processing the ResourceURI value
		if ((message.getResourceURI() == null)
				|| (message.getResourceURI().trim().length() == 0)) {
			// if defaults set then use them otherwise don't
			if ((settings.getResourceUri() != null)
					&& (settings.getResourceUri().trim().length() > 0)) {
				message.setResourceURI(settings.getResourceUri());
			}
		}
		// Processing for xmlBinding
		if (message.getXmlBinding() == null) {
			if (settings.getXmlBinding() != null) {
				message.setXmlBinding(settings.getXmlBinding());
			} else { // otherwise use/create default one for Management class
				if (defautInst != null) {
					message.setXmlBinding(defautInst.getXmlBinding());
				} else {
					message.setXmlBinding(new Management().getXmlBinding());
				}
			}
		}

		// Processing ReplyTo
		if ((settings.getReplyTo() != null)
				&& settings.getReplyTo().trim().length() > 0) {
			message.setReplyTo(settings.getReplyTo());
		} else {
			message.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
		}

		// Processing MessageId component
		if ((settings.getUidScheme() != null)
				&& (settings.getUidScheme().trim().length() > 0)) {
			message.setMessageId(settings.getUidScheme()
					+ UUID.randomUUID().toString());
		} else {
			message.setMessageId(ManagementMessageValues.DEFAULT_UID_SCHEME
					+ UUID.randomUUID().toString());
		}

		// Add processing for OperationTimeout
		final DatatypeFactory factory = DatatypeFactory.newInstance();
		if (settings.getTimeout() > 0) {
			message.setTimeout(factory.newDuration(settings.getTimeout()));
		}

		// process the selectors passed in.
		if ((settings.getSelectorSet() != null)
				&& (settings.getSelectorSet().size() > 0)) {
			message.setSelectors(settings.getSelectorSet());
		}

		// Processing MaxEnvelopeSize
		if ((settings.getMaxEnvelopeSize() != null)
				&& settings.getMaxEnvelopeSize().longValue() > 0) {
			final MaxEnvelopeSizeType maxEnvSize = Management.FACTORY
					.createMaxEnvelopeSizeType();
			maxEnvSize.setValue(settings.getMaxEnvelopeSize());
			maxEnvSize.getOtherAttributes()
					.put(SOAP.MUST_UNDERSTAND, SOAP.TRUE);
			message.setMaxEnvelopeSize(maxEnvSize);
		}

		// Processing Locale
		if (settings.getLocale() != null) {
			message.setLocale(settings.getLocale());
		}

		// Add processing for other Management components
		if ((settings.getAdditionalHeaders() != null)
				&& (settings.getAdditionalHeaders().size() > 0)) {
			final Iterator<ReferenceParametersType> iter = settings
					.getAdditionalHeaders().iterator();
			while (iter.hasNext()) {
				ReferenceParametersType element = (ReferenceParametersType) iter
						.next();
				message.addHeaders(element);
			}
		}

		// process the options passed in.
		if ((settings.getOptionSet() != null)
				&& (settings.getOptionSet().size() > 0)) {
			message.setOptions(settings.getOptionSet());
		}

		return message;
	}

	public static Management buildMessage(Management existing,
			Addressing subMessage, boolean trimAdditionalMetadata)
			throws SOAPException, JAXBException, DatatypeConfigurationException {
		// return reference
		Management message = null;
		// initialize if not already
		if (subMessage == null) {
			message = new Management();
		} else {// else use Addressing instance passed in.
			message = new Management(subMessage);
		}
		// Populate the new message instance with the values
		if ((existing != null) && (existing.getHeaders() != null)) {
			for (SOAPElement header : existing.getHeaders()) {
				// Don't add the original Action header
				QName examine = null;
				if (((examine = header.getElementQName()) != null)
						&& examine.getLocalPart().equals(
								Management.ACTION.getLocalPart())) {
					// Bail out and do not add.
					continue;
				}
				// Don't add the original MessageId if one exists
				QName mesgId = null;
				if (((mesgId = header.getElementQName()) != null)
						&& mesgId.getLocalPart().equals(
								Management.MESSAGE_ID.getLocalPart())) {
					// Bail out and do not add.
					continue;
				}
				if (trimAdditionalMetadata) {
					// if(!AnnotationProcessor.isDescriptiveMetadataElement(
					// header.getElementQName())){
					// Node located =
					// containsHeader(message.getHeader(),header);
					// if(located!=null){
					// message.getHeader().removeChild(located);
					// }
					// message.getHeader().addChildElement(header);
					// }
				} else {
					// message.getHeader().addChildElement(header);
					Node located = containsHeader(message.getHeader(), header);
					if (located != null) {
						message.getHeader().removeChild(located);
					}
					message.getHeader().addChildElement(header);
				}
			}
		}
		message = buildMessage(message, ManagementMessageValues.newInstance());
		return message;
	}

	/**
	 * Attempts to extract Selectors returned from a Management instance
	 * including a CreateResponse type, as a Map&lt;String,String&gt; for
	 * convenience.
	 * 
	 * @param managementMessage
	 * @return extracted selectors
	 * @throws SOAPException
	 * @throws JAXBException
	 */
	public static Map<String, String> extractSelectors(
			Management managementMessage) throws SOAPException, JAXBException {
		// stores located selectors
		Map<String, String> selectors = new HashMap<String, String>();

		// parse the Management instance passed in for ResourceCreated and
		// embedded selectors
		if (managementMessage != null) {
			EndpointReferenceType crtType = null;
			if ((managementMessage.getBody() != null)
					&& (managementMessage.getBody().getFirstChild() != null)) {
				// Extract dom component
				Node createContent = managementMessage.getBody()
						.getFirstChild();
				try {
					final JAXBElement<EndpointReferenceType> unmarshal =
						(JAXBElement<EndpointReferenceType>) binding.unmarshal(createContent);
					crtType = unmarshal.getValue();
				} catch (Exception ex) {
					ex.printStackTrace();
					LOG.warning(ex.getMessage());
				}
				if (crtType != null) {
					// extract the CreateResponseType instance
					EndpointReferenceType resCreatedElement = crtType;
					if ((resCreatedElement != null)
							&& (resCreatedElement.getReferenceParameters() != null)
							&& (resCreatedElement.getReferenceParameters()
									.getAny() != null)) {
						List<Object> refContents = resCreatedElement
								.getReferenceParameters().getAny();
						if ((refContents != null) && (refContents.size() > 0)) {
							for (Object node : refContents) {
								JAXBElement eprElement = (JAXBElement) node;
								// locate the refParameter element that is the
								// selectorSet
								if (eprElement.getName().getLocalPart().equals(
										Management.SELECTOR_SET.getLocalPart())) {
									Document nod = Management.newDocument();
									binding.marshal(node, nod);

									final JAXBElement selSet = (JAXBElement) binding
											.unmarshal(nod);
									SelectorSetType sels = (SelectorSetType) selSet
											.getValue();
									if (sels != null) {
										// extract the SelectorSet contents
										selectors = ManagementUtility
												.extractSelectorsAsMap(
														selectors, sels
																.getSelector());
									}
								}// end of if
							}
						}
					}
				}
			}
		}

		return selectors;
	}

	/**
	 * Convenience method to locate a specific SOAPElement from within the
	 * SOAPHeader instance.
	 * 
	 * @param header
	 * @param element
	 * @return the node
	 */
	private static Node containsHeader(SOAPHeader header, SOAPElement element) {
		Node located = null;
		NodeList chNodes = header.getChildNodes();
		QName elementNode = element.getElementQName();
		for (int i = 0; i < header.getChildNodes().getLength(); i++) {
			Node elem = chNodes.item(i);
			if ((elem.getLocalName().equals(elementNode.getLocalPart()))
					&& (elem.getNamespaceURI().equals(elementNode
							.getNamespaceURI()))) {
				located = elem;
			}
		}
		return located;
	}

	/**
	 * Extracts the addressing components from a Management message as an EPR
	 * type. NOTE: The original EPR used to construct a message cannot be fully
	 * reconstructed from the message as the metadata information marking the
	 * reference parameters and properties is lost when the EPR is flattened out
	 * into message headers.
	 * 
	 * @param message
	 *            Management message
	 * @return the Endpoint Reference extracted from the message
	 * @throws JAXBException
	 * @throws SOAPException
	 */
	public static EndpointReferenceType extractEprType(Management message)
			throws JAXBException, SOAPException {
		final EndpointReferenceType epr = addressing_factory
				.createEndpointReferenceType();

		// ######## Address field
		final AttributedURI to = addressing_factory.createAttributedURI();
		to.setValue(message.getTo());
		epr.setAddress(to);

		// ######## Reference Parameters
		final ReferenceParametersType refParams = addressing_factory
				.createReferenceParametersType();

		// add the resourceUri
		final SOAPElement resourceURI = ManagementUtility.locateHeader(message
				.getHeaders(), Management.RESOURCE_URI);
		if ((resourceURI != null)
				&& (resourceURI.getTextContent().trim().length() > 0)) {
			refParams.getAny().add(resourceURI);
		}

		// add the MetadataResourceUID if present.
		String NS_PREFIX = "wsmeta";
		String NS_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd";
		QName metResourceUID = new QName(NS_URI, "ResourceMetaDataUID",
				NS_PREFIX);
		SOAPElement metDataUID = ManagementUtility.locateHeader(message
				.getHeaders(), metResourceUID);
		if ((metDataUID != null)
				&& (metDataUID.getTextContent().trim().length() > 0)) {
			refParams.getAny().add(metDataUID);
		}

		// add the SelectorSet if defined
		final SOAPElement selectorSet = ManagementUtility.locateHeader(message
				.getHeaders(), Management.SELECTOR_SET);
		if ((selectorSet != null) && (selectorSet.hasChildNodes())) {
			refParams.getAny().add(selectorSet);
		}

		if ((refParams.getAny() != null) && (refParams.getAny().size() > 0)) {
			epr.setReferenceParameters(refParams);
		}

		// ######## Reference Properties cannot be reconstructed from the
		// headers
		return epr;
	}

	// ###################### GETTERS/SETTERS for instance
	/*
	 * Exposes the default uid scheme for the ManagementUtility instance.
	 * 
	 */
	public static String getUidScheme() {
		return uidScheme;
	}

	/**
	 * @return the defaultTimeout in milliseconds.
	 */
	public static long getDefaultTimeout() {
		return defaultTimeout;
	}

	/**
	 * Sets the default OperationTimeout.
	 * This is the default used if Timeout is &lt;=0.
	 * Value specified is in milliseconds.
	 * Default is 30000 milliseconds (30 seconds).
	 */
	public static void setDefaultTimeout(final long operationTimeout) {
		defaultTimeout = operationTimeout;
	}
	/**
	 * Send an HTTP request and return the response as a ResourceState
	 * 
	 * @param response
	 *            SOAP response
	 * @return SOAP response as a ResourceState
	 * @throws Exception
	 */
	public static ResourceStateDocument getAsResourceState(Addressing response)
			throws Exception {
		return new ResourceStateDocumentImpl(response.getEnvelope()
				.getOwnerDocument());

	}

	/**
	 * Send an http request and return the response as a Addressing object
	 * 
	 * @param request
	 *            SOAP request
	 * @return SOAP response as a ResourceState
	 * @throws Exception
	 */
	public static Addressing getRequest(Addressing request) throws Exception {
		// Send the get request to the server
		Addressing response = HttpClient.sendRequest(request);

		// Look for returned faults
		if (response.getBody().hasFault()) {
			SOAPFault fault = response.getBody().getFault();
			throw new SOAPException(fault.getFaultString());
		}

		return response;

	}

	public static String xmlToString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}

	// //
	// public static String locateClassInClasspath(String className) {
	// String message = "";
	// if (!className.startsWith("/")) {
	// className = "/" + className;
	// }
	// className = className.replace('.', '/');
	// className = className + ".class";
	//	  
	// java.net.URL classUrl =
	// new ManagementUtility().getClass().getResource(className);
	//	  
	// String exc = null;
	// if (classUrl != null) {
	// exc = "\nClass '" + className +
	// "' found in \n'" + classUrl.getFile() + "'";
	// System.out.println(exc);
	// } else {
	// exc = "\nClass '" + className +
	// "' not found in \n'";
	// exc+=System.getProperty("java.class.path") + "'";
	// System.out.println(exc);
	// }
	// message = exc;
	// return message;
	// }

}
