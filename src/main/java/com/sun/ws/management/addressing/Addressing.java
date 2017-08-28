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
 **$Log: Addressing.java,v $
 **Revision 1.18  2007/05/30 20:31:06  nbeers
 **Add HP copyright header
 **
 **
 * $Id: Addressing.java,v 1.18 2007/05/30 20:31:06 nbeers Exp $
 */

package com.sun.ws.management.addressing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.AttributedQName;
import org.xmlsoap.schemas.ws._2004._08.addressing.AttributedURI;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferencePropertiesType;
import org.xmlsoap.schemas.ws._2004._08.addressing.Relationship;
import org.xmlsoap.schemas.ws._2004._08.addressing.ServiceNameType;

import com.sun.ws.management.Management;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.xml.XmlBinding;

public class Addressing extends SOAP {

    public static final String NS_PREFIX = "wsa";
    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

    public static final String UNSPECIFIED_MESSAGE_ID = "http://schemas.xmlsoap.org/ws/2004/08/addressing/id/unspecified";
    public static final String ANONYMOUS_ENDPOINT_URI = "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous";
    public static final String FAULT_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/addressing/fault";

    public static final QName ACTION = new QName(NS_URI, "Action", NS_PREFIX);
    public static final QName TO = new QName(NS_URI, "To", NS_PREFIX);
    public static final QName MESSAGE_ID = new QName(NS_URI, "MessageID", NS_PREFIX);
    public static final QName REPLY_TO = new QName(NS_URI, "ReplyTo", NS_PREFIX);
    public static final QName FAULT_TO = new QName(NS_URI, "FaultTo", NS_PREFIX);
    public static final QName FROM = new QName(NS_URI, "From", NS_PREFIX);
    public static final QName ADDRESS = new QName(NS_URI, "Address", NS_PREFIX);
    public static final QName RELATES_TO = new QName(NS_URI, "RelatesTo", NS_PREFIX);
    public static final QName RETRY_AFTER = new QName(NS_URI, "RetryAfter", NS_PREFIX);
    public static final QName ENDPOINT_REFERENCE = new QName(NS_URI, "EndpointReference", NS_PREFIX);

    public static final ObjectFactory FACTORY = new ObjectFactory();

    public Addressing() throws SOAPException {
        super();
    }

    public Addressing(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public Addressing(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public Addressing(final SOAPMessage msg) throws SOAPException {
        super(msg);
    }

    public void validate() throws SOAPException, JAXBException, FaultException {
        validateElementPresent(getAction(), ACTION);
        validateElementPresent(getTo(), TO);
        validateElementPresent(getMessageId(), MESSAGE_ID);
        validateElementPresent(getReplyTo(), REPLY_TO);
        final String replyToAddress = getReplyTo().getAddress().getValue();
        validateElementPresent(replyToAddress, ADDRESS);

        validateURISyntax(getAction());
        validateURISyntax(getTo());
        validateURISyntax(getMessageId());
        validateURISyntax(replyToAddress);
    }

    protected void validateElementPresent(final Object element, final QName elementName) throws FaultException {
        if (element == null) {
//        	throw new MessageInformationHeaderRequiredFault(elementName);
        	String text = "'"+elementName.getLocalPart()+"' element missing.";
        	String faultDetail = elementName+" is required but was not found.";
        	Object[] details = {
        			"During validation the following ",
        			"required element '"+elementName+"'",
        			"could not be found. Unable to proceed with processing."};
            throw new MessageInformationHeaderRequiredFault(text,faultDetail,
            	null,elementName,details);
        }
    }

    protected void validateURISyntax(final String uri)
    throws FaultException {
        try {
            new URI(uri);
        } catch (URISyntaxException syntax) {
            throw new InvalidMessageInformationHeaderFault(uri);
        }
    }

    // only address is mandatory, the rest of the params are optional and can be null
    public static EndpointReferenceType createEndpointReference(final String address,
            final ReferencePropertiesType props, final ReferenceParametersType params,
            final AttributedQName portType, final ServiceNameType serviceName) {

        final EndpointReferenceType epr = FACTORY.createEndpointReferenceType();

        final AttributedURI addressURI = FACTORY.createAttributedURI();
        addressURI.getOtherAttributes().put(SOAP.MUST_UNDERSTAND, Boolean.TRUE.toString());
        addressURI.setValue(address.trim());
        epr.setAddress(addressURI);

        if (params != null) {
            epr.setReferenceParameters(params);
        }

        if (props != null) {
            epr.setReferenceProperties(props);
        }

        if (serviceName != null) {
            epr.setServiceName(serviceName);
        }

        if (portType != null) {
            epr.setPortType(portType);
        }

        return epr;
    }

    public Node[] unwrapEndpointReference(final Node wrappedEPR) {
        if (ENDPOINT_REFERENCE.getLocalPart().equals(wrappedEPR.getLocalName()) &&
                ENDPOINT_REFERENCE.getNamespaceURI().equals(wrappedEPR.getNamespaceURI())) {
            final NodeList children = wrappedEPR.getChildNodes();
            final List<Node> nl = new ArrayList<Node>(children.getLength());
            for (int i = children.getLength() - 1; i >= 0; i--) {
                nl.add(children.item(i));
            }
            return (Node[]) nl.toArray(new Node[nl.size()]);
        }
        throw new IllegalArgumentException("Can only unwrap EndpointReferences");
    }

    // setters

    public void addHeaders(final ReferenceParametersType params) throws JAXBException {
        if (params == null) {
            return;
        }
        addHeaders(params.getAny());
    }

    public void addHeaders(final ReferencePropertiesType props) throws JAXBException {
        if (props == null) {
            return;
        }
        addHeaders(props.getAny());
    }

    private void addHeaders(final List<Object> anyList) throws JAXBException {
        if (anyList == null) {
            return;
        }

        XmlBinding binding = getXmlBinding();
        final Node header = getHeader();
        for (final Object any : anyList) {
            if (any instanceof Node) {
                Node node = (Node) any;
                NodeList existingHeaders = null;
                Node hNode =null;
                //prevent duplicate additions.
                if(((existingHeaders = header.getChildNodes())!=null)
                		&&(existingHeaders.getLength()>0)){
                   for (int i = 0; i < existingHeaders.getLength(); i++) {
					 hNode = existingHeaders.item(i);
					 if((node.getNamespaceURI().equals(hNode.getNamespaceURI()))&
						(node.getLocalName().equals(hNode.getLocalName()))){
						header.removeChild(hNode);
					 }
                   }
                }
                // NOTE: can be a performance hog if the node is deeply nested
                header.appendChild(header.getOwnerDocument().importNode(node, true));
            } else {
            	try {
                    binding.marshal(any, header);
            	}
                catch (JAXBException e) {
                    throw new RuntimeException("RefP " + any.toString() +
                            " of class " + any.getClass() + " is being ignored");
                }
            }
        }
    }

    public void setAction(final String action) throws JAXBException, SOAPException {
        removeChildren(getHeader(), ACTION);
        final AttributedURI actionURI = FACTORY.createAttributedURI();
        actionURI.getOtherAttributes().put(SOAP.MUST_UNDERSTAND, Boolean.TRUE.toString());
        actionURI.setValue(action.trim());
        final JAXBElement<AttributedURI> actionElement = FACTORY.createAction(actionURI);
        getXmlBinding().marshal(actionElement, getHeader());
    }

    public void setTo(final String to) throws JAXBException, SOAPException {
        removeChildren(getHeader(), TO);
        final AttributedURI toURI = FACTORY.createAttributedURI();
        toURI.setValue(to.trim());
        toURI.getOtherAttributes().put(SOAP.MUST_UNDERSTAND, Boolean.TRUE.toString());
        final JAXBElement<AttributedURI> toElement = FACTORY.createTo(toURI);
        getXmlBinding().marshal(toElement, getHeader());
    }

    public void setMessageId(final String msgId) throws JAXBException, SOAPException {
        removeChildren(getHeader(), MESSAGE_ID);
        final AttributedURI msgIdURI = FACTORY.createAttributedURI();
        msgIdURI.getOtherAttributes().put(SOAP.MUST_UNDERSTAND, Boolean.TRUE.toString());
        msgIdURI.setValue(msgId.trim());
        final JAXBElement<AttributedURI> msgIdElement = FACTORY.createMessageID(msgIdURI);
        getXmlBinding().marshal(msgIdElement, getHeader());
    }

    // convenience method
    public void setReplyTo(final String uri) throws JAXBException, SOAPException {
        setReplyTo(createEndpointReference(uri.trim(), null, null, null, null));
    }

    public void setReplyTo(final EndpointReferenceType epr) throws JAXBException, SOAPException {
        removeChildren(getHeader(), REPLY_TO);
        final JAXBElement<EndpointReferenceType> element = FACTORY.createReplyTo(epr);
        getXmlBinding().marshal(element, getHeader());
    }

    // convenience method
    public void setFaultTo(final String uri) throws JAXBException, SOAPException {
        setFaultTo(createEndpointReference(uri.trim(), null, null, null, null));
    }

    public void setFaultTo(final EndpointReferenceType epr) throws JAXBException, SOAPException {
        removeChildren(getHeader(), FAULT_TO);
        final JAXBElement<EndpointReferenceType> element = FACTORY.createFaultTo(epr);
        getXmlBinding().marshal(element, getHeader());
    }

    // convenience method
    public void setFrom(final String uri) throws JAXBException, SOAPException {
        setFrom(createEndpointReference(uri.trim(), null, null, null, null));
    }

    public void setFrom(final EndpointReferenceType epr) throws JAXBException, SOAPException {
        removeChildren(getHeader(), FROM);
        final JAXBElement<EndpointReferenceType> element = FACTORY.createFrom(epr);
        getXmlBinding().marshal(element, getHeader());
    }

    public void addRelatesTo(final String relationshipURI) throws JAXBException {
        final Relationship relationship = FACTORY.createRelationship();
        relationship.setValue(relationshipURI.trim());
        final JAXBElement<Relationship> element = FACTORY.createRelatesTo(relationship);
        getXmlBinding().marshal(element, getHeader());
    }

    public void addRelatesTo(final String relationshipURI, final QName relationshipType) throws JAXBException {
        final Relationship relationship = FACTORY.createRelationship();
        relationship.setRelationshipType(relationshipType);
        relationship.setValue(relationshipURI.trim());
        final JAXBElement<Relationship> element = FACTORY.createRelatesTo(relationship);
        getXmlBinding().marshal(element, getHeader());
    }

    // getters

    public SOAPElement[] getHeaders() throws SOAPException {
        return getChildren(getHeader());
    }

    public String getAction() throws JAXBException, SOAPException {
        return getAttributedURI(ACTION);
    }

    public String getTo() throws JAXBException, SOAPException {
        return getAttributedURI(TO);
    }

    public String getMessageId() throws JAXBException, SOAPException {
        return getAttributedURI(MESSAGE_ID);
    }

    public EndpointReferenceType getReplyTo() throws JAXBException, SOAPException {
        return getEndpointReference(REPLY_TO);
    }

    public EndpointReferenceType getFaultTo() throws JAXBException, SOAPException {
        return getEndpointReference(FAULT_TO);
    }

    public EndpointReferenceType getFrom() throws JAXBException, SOAPException {
        return getEndpointReference(FROM);
    }

    public Relationship[] getRelatesTo() throws JAXBException, SOAPException {
        final SOAPElement[] relations = getChildren(getHeader(), RELATES_TO);
        final Relationship[] relationships = new Relationship[relations.length];
        for (int i=0; i < relations.length; i++) {
            final Object relation = getXmlBinding().unmarshal(relations[i]);
            relationships[i] = (Relationship)(((JAXBElement) relation).getValue());
        }
        return relationships;
    }

    public EndpointReferenceType getEndpointReference(final SOAPElement parent, final QName... qname) throws JAXBException, SOAPException {
        final Object value = unbind(parent, qname);
        return value == null ? null : (EndpointReferenceType)(((JAXBElement) value).getValue());
    }

    // get helpers

    private String getAttributedURI(final QName qname) throws JAXBException, SOAPException {
        final JAXBElement value = (JAXBElement)unbind(getHeader(), qname);
        return value == null ? null : ((AttributedURI)(value.getValue())).getValue().trim();
    }

    private EndpointReferenceType getEndpointReference(final QName... qname) throws JAXBException, SOAPException {
        return getEndpointReference(getHeader(), qname);
    }

    /** This is a convenience method to create a ReferencePropertyType from a QName and content
     *  value.
     *
     * @param container QName
     * @param content
     * @return ReferencePropertyType instance.
     */
    public static ReferencePropertiesType createReferencePropertyType(QName container, String content) {
    	//test for invalid input.
    	if((container==null)||(content==null)){
    		throw new IllegalArgumentException("QName and/or string content cannot be null.");
    	}
    	if(content.trim().length()==0){
    		throw new IllegalArgumentException("Content entered cannot be an empty string.");
    	}

    	//create and populate the reference property and it's value.
    	final ReferencePropertiesType refProperty = Addressing.FACTORY.createReferencePropertiesType();
    	final Document document = Management.newDocument();
    	final Element identifier = document.createElementNS(container.getNamespaceURI(),
    			container.getPrefix() + ":" + container.getLocalPart());
    	identifier.setTextContent(content);
    	document.appendChild(identifier);
    	refProperty.getAny().add(document.getDocumentElement());

    	return refProperty;
    }

    /** This is a convenience method to create a ReferenceParametersType from a QName and content
     *  value.
     *
     * @param container QName
     * @param content
     * @return ReferenceParametersType instance.
     */
    public static ReferenceParametersType createReferenceParametersType(QName container, String content) {
    	//test for invalid input.
    	if((container==null)||(content==null)){
    		throw new IllegalArgumentException("QName and/or string content cannot be null.");
    	}
    	if(content.trim().length()==0){
    		throw new IllegalArgumentException("Content entered cannot be an empty string.");
    	}

    	//create and populate the reference property and it's value.
    	final ReferenceParametersType refParameter = Addressing.FACTORY.createReferenceParametersType();
    	final Document document = Management.newDocument();
    	final Element identifier = document.createElementNS(container.getNamespaceURI(),
    			container.getPrefix() + ":" + container.getLocalPart());
    	 identifier.setTextContent(content);
    	 document.appendChild(identifier);
    	 refParameter.getAny().add(document.getDocumentElement());

    	return refParameter;
    }


}
