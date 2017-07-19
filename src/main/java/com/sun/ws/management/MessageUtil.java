/*
 * Copyright 2006-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.ws.management;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.addressing.DestinationUnreachableFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.server.EnumerationSupport;
import com.sun.ws.management.server.EventingSupport;
import com.sun.ws.management.server.NamespaceMap;
import com.sun.ws.management.server.WSEnumerationSupport;
import com.sun.ws.management.server.message.WSManagementRequest;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.xml.XML;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.util.JAXBResult;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.EndpointReference;
import org.dmtf.schemas.wbem.wsman._1.wsman.AnyListType;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributablePositiveInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.ItemType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.eventing.DeliveryType;
import org.xmlsoap.schemas.ws._2004._08.eventing.FilterType;
import org.xmlsoap.schemas.ws._2004._08.eventing.LanguageSpecificStringType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.SubscriptionEnd;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

/**
 *
 * @author jfdenise
 */
public class MessageUtil {
    private MessageUtil() {}
    
    public static Subscribe createSubscribe(final EndpointReferenceType endTo, final String deliveryMode,
            final EndpointReferenceType notifyTo, final String expires, final FilterType filter,
            final Object... extensions)
            throws SOAPException, JAXBException {
        final Subscribe sub = Eventing.FACTORY.createSubscribe();
        
        if (endTo != null) {
            sub.setEndTo(endTo);
        }
        
        final DeliveryType delivery = Eventing.FACTORY.createDeliveryType();
        
        if (deliveryMode != null) {
            delivery.setMode(deliveryMode);
        }
        
        if (notifyTo != null) {
            delivery.getContent().add((Eventing.FACTORY.createNotifyTo(notifyTo)));
        }
        
        if (extensions != null) {
            for (final Object ext : extensions) {
                delivery.getContent().add(ext);
            }
        }
        
        sub.setDelivery(delivery);
        
        if (expires != null) {
            sub.setExpires(expires);
        }
        
        if (filter != null) {
            sub.setFilter(filter);
        }
        
        return sub;
    }
    
    public static List createEnumerateList(final boolean optimize,
            final int maxElements,
            final DialectableMixedDataType filter,
            final EnumerationExtensions.Mode mode,
            final Object... anys) throws JAXBException, SOAPException {
        
        ArrayList<Object> list = new ArrayList<Object>();
        
        if (filter != null) {
            list.add(Management.FACTORY.createFilter(filter));
        }
        if (mode != null) {
            list.add(mode.toBinding());
        }
        if (optimize == true) {
            list.add(Management.FACTORY.createOptimizeEnumeration(new AttributableEmpty()));
            
            if (maxElements > 0) {
                final AttributablePositiveInteger posInt = new AttributablePositiveInteger();
                posInt.setValue(new BigInteger(Integer.toString(maxElements)));
                list.add(Management.FACTORY.createMaxElements(posInt));
            }
        }
        
        for (int i = 0; i < anys.length; i++) {
            list.add(anys[i]);
        }
        
        return list;
    }
    
    public static Object extract(final List<Object> anyList, final Class classType, final QName eltName) {
        for (final Object any : anyList) {
            if (any instanceof JAXBElement) {
                final JAXBElement elt = (JAXBElement) any;
                if ((classType != null && classType.equals(elt.getDeclaredType())) &&
                        (eltName != null && eltName.equals(elt.getName()))) {
                    return elt.getValue();
                }
            }
        }
        return null;
    }
    private static EnumerationItem unbindItem(Object obj)
    throws JAXBException {
        // the three possibilities are: EPR only, Item and EPR or Item only
        
        Object item = null;
        EndpointReferenceType eprt = null;
        if (obj instanceof JAXBElement) {
            final JAXBElement elt = (JAXBElement) obj;
            if (EndpointReferenceType.class.equals(elt.getDeclaredType())) {
                // EPR only
                eprt = ((JAXBElement<EndpointReferenceType>) obj).getValue();
            } else {
                if (ItemType.class.equals(elt.getDeclaredType())) {
                    // Item and EPR
                    final ItemType wsmanItem = ((JAXBElement<ItemType>) obj)
                    .getValue();
                    final List<Object> content = wsmanItem.getContent();
                    final Iterator iter = content.iterator();
                    while (iter.hasNext()) {
                        Object itemObj = iter.next();
                        // XXX Revisit, JAXB is adding an empty String when unmarshaling
                        // a Mixed content. getContent() returns a list containing
                        // such empty String element
                        // BUG ID is : 6542005
                        // Unmarshalled @XmlMixed list contains an additional empty String
                        if(itemObj instanceof String) {
                            // Having the list being of Mixed content
                            // An empty string element is added.
                            String str = (String) itemObj;
                            if(str.length() == 0)
                                continue;
                            else
                                item = itemObj;
                        } else
                            if ((itemObj instanceof JAXBElement)
                            && ((JAXBElement) itemObj).getDeclaredType()
                            .equals(EndpointReferenceType.class)) {
                            final JAXBElement<EndpointReferenceType> jaxbEpr = (JAXBElement<EndpointReferenceType>) itemObj;
                            eprt = jaxbEpr.getValue();
                            } else {
                            item = itemObj;
                            }
                    }
                } else {
                    // JAXB Item only
                    item = elt;
                }
            }
        } else {
            // Item only
            item = obj;
        }
        return new EnumerationItem(item, eprt);
    }
    
    public static List<EnumerationItem> getItems(PullResponse pullResponse,
            EnumerateResponse enumerateResponse) throws JAXBException, SOAPException {
        final List<Object> items;
        if (pullResponse != null) {
            final ItemListType list = pullResponse.getItems();
            if (list == null) {
                return null;
            }
            items = list.getAny();
        } else {
            if (enumerateResponse != null) {
                final Object obj = extract(enumerateResponse.getAny(),
                        AnyListType.class, EnumerationExtensions.ITEMS);
                if (obj instanceof AnyListType) {
                    items = ((AnyListType) obj).getAny();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        if (items == null) {
            return null;
        }
        final int size = items.size();
        final List<EnumerationItem> itemList = new ArrayList<EnumerationItem>();
        for (int i = 0; i < size; i++) {
            final Object object = items.get(i);
            itemList.add(unbindItem(object));
        }
        return itemList;
    }
    
    public static boolean isEndOfSequence(PullResponse pullResponse, EnumerateResponse enumerateResponse)
    throws JAXBException, SOAPException {
        final Object eos;
        if (pullResponse != null) {
            eos = pullResponse.getEndOfSequence();
        } else {
            if (enumerateResponse != null) {
                eos = MessageUtil.extract(enumerateResponse.getAny(),
                        AttributableEmpty.class, EnumerationExtensions.END_OF_SEQUENCE);
            } else {
                return false;
            }
        }
        return null != eos;
    }
    public static SubscriptionEnd createSubscriptionEnd(final EndpointReferenceType mgr,
            final String status, final String reason) throws SOAPException, JAXBException {
        
        if (!Eventing.DELIVERY_FAILURE_STATUS.equals(status) &&
                !Eventing.SOURCE_SHUTTING_DOWN_STATUS.equals(status) &&
                !Eventing.SOURCE_CANCELING_STATUS.equals(status)) {
            throw new IllegalArgumentException("Status must be one of " +
                    Eventing.DELIVERY_FAILURE_STATUS + ", " +
                    Eventing.SOURCE_SHUTTING_DOWN_STATUS + " or " +
                    Eventing.SOURCE_CANCELING_STATUS);
        }
        final SubscriptionEnd end = Eventing.FACTORY.createSubscriptionEnd();
        end.setSubscriptionManager(mgr);
        end.setStatus(status);
        
        if (reason != null) {
            final LanguageSpecificStringType localizedReason = Eventing.FACTORY.createLanguageSpecificStringType();
            localizedReason.setLang(XML.DEFAULT_LANG);
            localizedReason.setValue(reason);
            end.getReason().add(localizedReason);
        }
        return end;
    }
    public static Enumerate createEnumerate(final EndpointReferenceType endTo,
            final String expires,
            final org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType filter,
            final Object... anys)
            throws JAXBException, SOAPException {
        final Enumerate enu = Enumeration.FACTORY.createEnumerate();
        if (endTo != null) {
            enu.setEndTo(endTo);
        }
        if (expires != null) {
        	final String exp = expires.trim();
        	if (exp.length() > 0)
                enu.setExpires(exp);
        }
        if (filter != null) {
            enu.setFilter(filter);
        }
        if (anys != null) {
            for (final Object any : anys) {
                if (any != null) {
                    enu.getAny().add(any);
                }
            }
        }
        return enu;
    }
    
    public static JAXBElement<DialectableMixedDataType> createFragmentHeader(final Object expression,
            final Map<String, String> namespaces,
            final String dialect)
            throws SOAPException, JAXBException {
        if (expression == null)
            return null;
        
        final DialectableMixedDataType dialectableMixedDataType = Management.FACTORY
                .createDialectableMixedDataType();
        if (dialect != null) {
            dialectableMixedDataType.setDialect(dialect);
        }
        
        dialectableMixedDataType.getOtherAttributes().put(SOAP.MUST_UNDERSTAND,
                Boolean.TRUE.toString());
        
        // add the query string to the content of the FragmentTransfer Header
        dialectableMixedDataType.getContent().add(expression);
        
        final JAXBElement<DialectableMixedDataType> fragmentTransfer = Management.FACTORY
                .createFragmentTransfer(dialectableMixedDataType);
        return fragmentTransfer;
    }
    
    public static Pull createPull(final Object context, final int maxChars,
            final int maxElements, final Duration maxDuration)
            throws JAXBException, SOAPException, DatatypeConfigurationException {
        final Pull pull = Enumeration.FACTORY.createPull();
        
        final EnumerationContextType contextType = Enumeration.FACTORY.createEnumerationContextType();
        contextType.getContent().add(context);
        pull.setEnumerationContext(contextType);
        
        if (maxChars > 0) {
            pull.setMaxCharacters(BigInteger.valueOf((long) maxChars));
        }
        if (maxElements > 0) {
            pull.setMaxElements(BigInteger.valueOf((long) maxElements));
        }
        if (maxDuration != null) {
            pull.setMaxTime(maxDuration);
        }
        return pull;
    }
    
    public static JAXBElement<MixedDataType> buildXmlFragment(final List<Object> content) throws SOAPException {
        //build the JAXB Wrapper Element
        final MixedDataType mixedDataType = Management.FACTORY.createMixedDataType();
        final StringBuffer buf = new StringBuffer();
        
        for (int j = 0; j < content.size(); j++) {
            // Check if it is a text node from text() function
            if (content.get(j) instanceof Text) {
                buf.append(((Text)content.get(j)).getTextContent());
            } else {
                if (buf.length() > 0) {
                    mixedDataType.getContent().add(buf.toString());
                    buf.setLength(0);
                }
                mixedDataType.getContent().add(content.get(j));
            }
        }
        if (buf.length() > 0) {
            mixedDataType.getContent().add(buf.toString());
            buf.setLength(0);
        }
        //create the XmlFragmentElement
        final JAXBElement<MixedDataType> xmlFragment =
                Management.FACTORY.createXmlFragment(mixedDataType);
        //if there was no content, then this is NIL
        if (content.size() <= 0) {
            xmlFragment.setNil(true);
        }
        return xmlFragment;
    }
    
    public static URI checkAddressURI(String to)
    throws JAXBException, SOAPException, FaultException {
        URI addressURI = null;
        try {
            if (to == null)
                throw new DestinationUnreachableFault(
                        "Unknown address URI: " + to);
            addressURI = new URI(to);
        } catch (Exception ex) { // (ckeck done by wiseman),
            // Throw a fault in conformance with DSP0226 - R2.1.1-5 & R2.1.1-8
            // and DSP0227 - R5.1-1
            if(ex instanceof MalformedURLException ||
                    ex instanceof UnmarshalException ||
                    ex instanceof URISyntaxException)
                throw new DestinationUnreachableFault(SOAP.createFaultDetail(
                        "Invalid To syntax", null,
                        ex, null));
        }
        
        return addressURI;
    }
    
    public static void addEnumerationItem(List<Object> itemListAny,
            EnumerationItem ee,
            EnumerationModeType mode) throws JAXBException {
        if (mode == null) {
            itemListAny.add(ee.getItem());
        } else if (EnumerationModeType.ENUMERATE_EPR.equals(mode)) {
            itemListAny.add(Addressing.FACTORY.createEndpointReference(ee.getEndpointReference()));
        } else if (EnumerationModeType.ENUMERATE_OBJECT_AND_EPR.equals(mode)) {
            final ItemType item = Management.FACTORY.createItemType();
            final Object obj = ee.getItem();
            
            // add the object to the Item
            if (obj instanceof Document) {
                item.getContent().add(((Document)obj).getDocumentElement());
            } else {
                // add the object as it is
                item.getContent().add(obj);
            }
            // add the EPR to the Item
            item.getContent().add(Addressing.FACTORY.
                    createEndpointReference(ee.getEndpointReference()));
            itemListAny.add(Management.FACTORY.createItem(item));;
        }
    }
    
    public static NamespaceContext getNamespaceContext(WSManagementRequest req) throws Exception {
        String action = req.getActionURI().toString();
        if(action.equals(Enumeration.ENUMERATE_ACTION_URI)) {
            return WSEnumerationSupport.getNamespaceMap(req);
        }
        if(action.equals(Eventing.SUBSCRIBE_ACTION_URI)) {
            return EventingSupport.getNamespaceMap(req);
        }
        if(action.equals(Transfer.CREATE_ACTION_URI) ||
                action.equals(Transfer.DELETE_ACTION_URI) ||
                action.equals(Transfer.GET_ACTION_URI) ||
                action.equals(Transfer.PUT_ACTION_URI)) {
            return new NamespaceMap(req.toSOAPMessage().getSOAPBody());
        }
        // Default case is similar to WS-Transfer one
        return new NamespaceMap(req.toSOAPMessage().getSOAPBody());
    }
    
    public static URI checkResourceURI(String resourceName)
    throws FaultException {
        URI resourceURI = null;
        try {
            if (resourceName == null)
                // Throw a fault in conformance with DSP0226 - R2.1.1-5 &
                // R2.1.1-8 and DSP0227 - R5.1-1
                throw new DestinationUnreachableFault(
                        "No ResourceURI",
                        DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
            resourceURI = new URI(resourceName);
            
        } catch (URISyntaxException ex) {   // (more check done by java.net.URI
            // ex: http:// passes wiseman but not URI.
            // Throw a fault in conformance with DSP0226 - R2.1.1-5 & R2.1.1-8
            // and DSP0227 - R5.1-1
            throw new DestinationUnreachableFault(SOAP.createFaultDetail(
                    "Invalid ResourceURI syntax",
                    DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI.toString(),
                    ex, null));
        }
        return resourceURI;
    }
    public static EndpointReferenceType toEPRType(JAXBContext wsaContext,
            EndpointReference epr) throws Exception {
        if(epr == null) return null;
        MemberSubmissionEndpointReference translated;
        if(epr instanceof MemberSubmissionEndpointReference)
            translated = (MemberSubmissionEndpointReference) epr;
        else {
            WSEndpointReference wsepr = new WSEndpointReference(epr);
            translated = wsepr.toSpec(MemberSubmissionEndpointReference.class);
        }
        JAXBResult res = new JAXBResult(wsaContext);
        
        translated.writeTo(res);
        JAXBElement<EndpointReferenceType> result =
                (JAXBElement<EndpointReferenceType>)res.getResult();
        
        EndpointReferenceType eprt = result.getValue();
        
        return eprt;
    }
}
