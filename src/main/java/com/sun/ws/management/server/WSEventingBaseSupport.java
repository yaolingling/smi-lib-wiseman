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
 **Revision 1.2  2007/11/07 11:15:36  denis_rachal
 **Issue number:  142 & 146
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **
 **142: EventingSupport.retrieveContext(UUID) throws RuntimeException
 **
 **Fixed WSEventingSupport to not throw RuntimeException. Instead it throws a new InvalidSubscriptionException. EventingSupport methods still throw RuntimeException to maintain backward compatibility.
 **
 **146: Enhance to allow specifying default expiration per enumeration
 **
 **Also enhanced WSEventingSupport to allow setting the default expiration per subscription. Default if not set by developer or client is now 24 hours for subscriptions.
 **
 **Additionally added javadoc to both EventingSupport and WSEventingSupport.
 **
 **Revision 1.1  2007/10/31 12:25:38  jfdenise
 **Split between new support and previous one.
 **
 **Revision 1.27  2007/10/30 09:27:47  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.26  2007/10/02 10:43:44  jfdenise
 **Fix for bug ID 134, Enumeration Iterator look up is static
 **Applied to Enumeration and Eventing
 **
 **Revision 1.25  2007/09/18 13:06:56  denis_rachal
 **Issue number:  129, 130 & 132
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **
 **129  ENHANC  P2  All  denis_rachal  NEW   Need support for ReNew Operation in Eventing
 **130  DEFECT  P3  x86  jfdenise  NEW   Should return a boolean variable result not a constant true
 **132  ENHANC  P3  All  denis_rachal  NEW   Make ServletRequest attributes available as properties in Ha
 **
 **Added enhancements and fixed issue # 130.
 **
 **Revision 1.24  2007/06/13 13:19:02  jfdenise
 **Fix for BUG ID 115 : EventingSupport should be able to create an event msg
 **
 **Revision 1.23  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSEventingBaseSupport.java,v 1.4 2007-12-20 20:47:52 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathException;

import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferencePropertiesType;

import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.CannotProcessFilterFault;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.eventing.InvalidSubscriptionException;
import com.sun.ws.management.server.message.WSEventingRequest;
import com.sun.ws.management.soap.SOAP;

/**
 * A helper class that encapsulates some of the arcane logic to manage
 * subscriptions using the WS-Eventing protocol.
 */
public class WSEventingBaseSupport extends BaseSupport {
    
    public static final int DEFAULT_QUEUE_SIZE = 1024;
    public static final int DEFAULT_EXPIRATION_MILLIS = 60000;
    
    // TODO: add more delivery modes as they are implemented
    private static final String[] SUPPORTED_DELIVERY_MODES = {
        Eventing.PUSH_DELIVERY_MODE,
        EventingExtensions.PULL_DELIVERY_MODE
    };
    
    protected WSEventingBaseSupport() {}
    
    public static String[] getSupportedDeliveryModes() {
        return SUPPORTED_DELIVERY_MODES;
    }
    
    public static boolean isDeliveryModeSupported(final String deliveryMode) {
        for (final String mode : SUPPORTED_DELIVERY_MODES) {
            if (mode.equals(deliveryMode)) {
                return true;
            }
        }
        return false;
    }
    
    public static NamespaceMap getNamespaceMap(final WSEventingRequest request) {
        final NamespaceMap nsMap;
        SOAPBody body;
        try {
            body = request.toSOAPMessage().getSOAPBody();
        }catch(Exception ex) {
           throw new RuntimeException(ex.toString());
        }
        
        NodeList wsmanFilter = body.getElementsByTagNameNS(EventingExtensions.FILTER.getNamespaceURI(),
                EventingExtensions.FILTER.getLocalPart());
        NodeList evtFilter = body.getElementsByTagNameNS(Eventing.FILTER.getNamespaceURI(),
                Eventing.FILTER.getLocalPart());
        if ((wsmanFilter != null) && (wsmanFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(wsmanFilter.item(0));
        } else if ((evtFilter != null) && (evtFilter.getLength() > 0)) {
            nsMap = new NamespaceMap(evtFilter.item(0));
        } else {
            NodeList evtElement = body.getElementsByTagNameNS(Eventing.SUBSCRIBE.getNamespaceURI(),
                    Eventing.SUBSCRIBE.getLocalPart());
            nsMap = new NamespaceMap(evtElement.item(0));
        }
        return nsMap;
    }
    
    protected static BaseContext retrieveContext(UUID id) 
                     throws InvalidSubscriptionException {
        assert datatypeFactory != null : UNINITIALIZED;
        
        BaseContext bctx = contextMap.get(id);
        if ((bctx == null) || (bctx.isDeleted())) {
            throw new InvalidSubscriptionException("Context not found: subscription does not exist");
        }
        
        // Check if context is expired
        final GregorianCalendar now = new GregorianCalendar();
        final XMLGregorianCalendar nowXml = datatypeFactory.newXMLGregorianCalendar(now);
        if (bctx.isExpired(nowXml)) {
            removeContext(null, bctx);
            throw new InvalidSubscriptionException("Subscription expired");
        }
        return bctx;
    }
    
    protected static Addressing createPushEventMessage(BaseContext bctx,
            Object content)
            throws SOAPException, JAXBException, IOException, InvalidSubscriptionException {
        // Push mode, send the data
        if (!(bctx instanceof EventingContext)) {
            throw new InvalidSubscriptionException("Context not found");
        }
        final EventingContext ctx = (EventingContext) bctx;
        
        final Addressing msg = new Addressing();
        msg.setAction(Management.EVENT_URI);
        
        if (ctx.getFilter() == null) {
            if (content instanceof Node)
                msg.getBody().appendChild(msg.getBody().getOwnerDocument().importNode((Node)content, true));
            else {
                msg.getXmlBinding().marshal(content, msg.getBody());
            }
        } else {
            final Element item;
            
            // Convert the content to an Element
            if (content instanceof Element) {
                item = (Element) content;
            } else if (content instanceof Document) {
                item = ((Document) content).getDocumentElement();
                // append the Element to the owner document
                // if it has not been done
                // this is critical for XPath filtering to work
                final Document owner = item.getOwnerDocument();
                if (owner.getDocumentElement() == null) {
                    owner.appendChild(item);
                }
            } else {
                Document doc = Management.newDocument();
                try {
                    // TODO: Use a better binding...
                    msg.getXmlBinding().marshal(content, doc);
                } catch (Exception e) {
                    removeContext(null, ctx);
                    final String explanation = "XML Binding marshall failed for object of type: "
                            + content.getClass().getName();
                    throw new InternalErrorFault(SOAP
                            .createFaultDetail(explanation, null,
                            e, null));
                }
                item = doc.getDocumentElement();
            }
            final NodeList filteredContent;
            try {
                filteredContent = ctx.evaluate(item);
            } catch (XPathException xpx) {
                removeContext(null, ctx);
                throw new CannotProcessFilterFault(
                        "Error evaluating XPath: "
                        + xpx.getMessage());
            } catch (Exception ex) {
                removeContext(null, ctx);
                throw new CannotProcessFilterFault(
                        "Error evaluating Filter: "
                        + ex.getMessage());
            }
            if ((filteredContent != null) && (filteredContent.getLength() > 0)) {
                // Then send this instance
                if (filteredContent.item(0).equals(item)) {
                    // Whole node was selected
                    final Document doc = msg.newDocument();
                    doc.appendChild(item);
                    msg.getBody().addDocument(doc);
                } else {
                    // Fragment(s) selected
                    final JAXBElement<MixedDataType> fragment = createXmlFragment(filteredContent);
                    msg.getXmlBinding().marshal(fragment, msg.getBody());
                }
                final String nsURI = item.getNamespaceURI();
                final String nsPrefix = item.getPrefix();
                if (nsPrefix != null && nsURI != null) {
                    msg.getBody().addNamespaceDeclaration(nsPrefix, nsURI);
                }
            } else {
                return null;
            }
        }
        final EndpointReferenceType notifyTo = ctx.getNotifyTo();
        msg.setTo(notifyTo.getAddress().getValue());
        final ReferenceParametersType refparams = notifyTo.getReferenceParameters();
        if (refparams != null) {
            msg.addHeaders(refparams);
        }
        final ReferencePropertiesType refprops = notifyTo.getReferenceProperties();
        if (refprops != null) {
            msg.addHeaders(refprops);
        }
        msg.setMessageId(UUID_SCHEME + UUID.randomUUID().toString());
        
        return msg;
    }
        
    /**
     * Retrieve the Context associated to passed ID, then create
     * the WS-Man request for the provided content.
     */
    public static Addressing createPushEventMessage(UUID id, Object content)
    throws SOAPException, JAXBException, IOException, InvalidSubscriptionException {
        
        BaseContext bctx = retrieveContext(id);
        return createPushEventMessage(bctx, content);
    }
}
