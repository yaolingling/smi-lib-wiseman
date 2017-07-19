/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 * 
 *  ** Copyright (C) 2006-2008 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Denis Rachal (denis.rachal@hp.com)
 **
 */

package com.sun.ws.management.server.message;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.EndpointReference;

import org.dmtf.schemas.wbem.wsman._1.wsman.AnyListType;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableNonNegativeInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.dmtf.schemas.wbem.wsman.identity._1.wsmanidentity.IdentifyResponseType;
import org.dmtf.schemas.wbem.wsman.identity._1.wsmanidentity.ObjectFactory;
import org.w3._2003._05.soap_envelope.Detail;
import org.w3._2003._05.soap_envelope.Fault;
import org.w3._2003._05.soap_envelope.Faultcode;
import org.w3._2003._05.soap_envelope.Faultreason;
import org.w3._2003._05.soap_envelope.Reasontext;
import org.w3._2003._05.soap_envelope.Subcode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.eventing.RenewResponse;
import org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import com.sun.ws.management.Management;
import com.sun.ws.management.MessageUtil;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.xml.XML;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;
import com.sun.xml.ws.message.StringHeader;
import com.sun.xml.ws.message.jaxb.JAXBHeader;
/**
 *
 * @author jfdenise
 */
public class JAXWSMessageResponse implements WSManagementResponse {
    
    private String action;
    private Fault fault;
    private Object payload;
    private JAXBRIContext jaxbContext;
    private JAXBRIContext payloadContext;
    private HeaderList extraHeaders;
    private HeaderList headers = new HeaderList();
    private static ObjectFactory IDENTIFY_FACTORY = new ObjectFactory();
    private static DocumentBuilder docBuilder;
    private static JAXBRIContext eprContext;
    
    /** Creates a new instance of WSMessageResponse */
    public JAXWSMessageResponse(JAXBRIContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }
    private static JAXBRIContext getEPRContext() throws Exception {
        if(eprContext == null)
          eprContext = (JAXBRIContext)JAXBContext.newInstance(MemberSubmissionEndpointReference.class);
        return eprContext;
    }
    public boolean containsFault() {
        return fault != null;
    }
    
    public void setFault(final FaultException ex) throws Exception {
        setFault(ex.getAction(), ex.getCode(), ex.getSubcode(), ex.getReason(), ex.getDetails());
        // allow subclasses an opportunity to encode additional information
    }
    
    private void setFault(final String action, final QName code,
            final QName subcode, final String reason,
            final Node... details) throws Exception {
        
        if (action != null) {
            this.setAction(action);
        }
        
        final Fault fault = SOAP.FACTORY.createFault();
        
        // keep a ref
        this.fault = fault;
        
        final Faultcode faultcode = SOAP.FACTORY.createFaultcode();
        faultcode.setValue(code);
        fault.setCode(faultcode);
        
        if (subcode != null) {
            final Subcode faultsubcode = SOAP.FACTORY.createSubcode();
            faultsubcode.setValue(subcode);
            faultcode.setSubcode(faultsubcode);
        }
        
        final Reasontext reasontext = SOAP.FACTORY.createReasontext();
        reasontext.setValue(reason);
        reasontext.setLang(XML.DEFAULT_LANG);
        final Faultreason faultreason = SOAP.FACTORY.createFaultreason();
        faultreason.getText().add(reasontext);
        fault.setReason(faultreason);
        
        final Detail faultdetail = SOAP.FACTORY.createDetail();
        final List<Object> detailsList = faultdetail.getAny();
        if (details != null) {
            for (final Node detail : details) {
                detailsList.add(detail);
            }
            fault.setDetail(faultdetail);
        }
        
        final JAXBElement<Fault> faultElement = SOAP.FACTORY.createFault(fault);
        payload = faultElement;
    }
    
    public void setIdentifyResponse(final String vendor, final String productVersion,
            final String protocolVersion, final Map<QName, String> more) throws Exception {
        IdentifyResponseType rt = new IdentifyResponseType();
        rt.setProductVendor(vendor);
        rt.setProductVersion(productVersion);
        rt.getProtocolVersion().add(protocolVersion);
        
        if (more != null) {
            if(docBuilder == null) {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            }
            Document doc = docBuilder.newDocument();
            final Iterator<Map.Entry<QName, String> > mi = more.entrySet().iterator();
            while (mi.hasNext()) {
                final Map.Entry<QName, String> entry = mi.next();
                Element m = doc.createElementNS(entry.getKey().getNamespaceURI(), entry.getKey().getLocalPart());
                Text t = doc.createTextNode(entry.getValue());
                m.appendChild(t);
                rt.getAny().add(m);
            }
        }
        payload = IDENTIFY_FACTORY.createIdentifyResponse(rt);
    }
    
    public void addNamespaceDeclaration(String nsPrefix,
            String nsURI) {
        //
    }
    
    public JAXBContext getJAXBContext() {
        return jaxbContext;
    }
    
    public void setFragmentResponse(DialectableMixedDataType header,
            List<Object> values, JAXBContext context) throws Exception {
        if(values != null) {
            JAXBElement<MixedDataType> mixed =
                    MessageUtil.buildXmlFragment(values);
            payload = mixed;
            payloadContext = (JAXBRIContext)context;
        }
        
        JAXBElement<DialectableMixedDataType> jaxb =
                Management.FACTORY.createFragmentTransfer(header);
        headers.add(Headers.create(jaxbContext, jaxb));
    }
    
    public Message buildMessage() throws IllegalArgumentException {
        Message msg;
        if(payload == null)
            msg = Messages.createEmpty(SOAPVersion.SOAP_12);
        else
            if(payload instanceof Element)
                msg = Messages.createUsingPayload((Element) payload, SOAPVersion.SOAP_12);
            else {
                JAXBRIContext ctx = payloadContext != null ? payloadContext :  jaxbContext;
                msg = Messages.create(ctx, payload, SOAPVersion.SOAP_12);
            }
        
        //Headers
        HeaderList headerList = msg.getHeaders();
        if(action != null) {
            //Headers
            headerList.add(new StringHeader(Addressing.ACTION, action)); 
        } else
            if(!isIdentifyResponse())
                throw new IllegalArgumentException("Can't create Message for null Action");
        
        if(extraHeaders != null)
            headerList.addAll(extraHeaders);
        
        if(headers.size() > 0)
            headerList.addAll(headers);
        
        return msg;
    }
    
    private boolean isIdentifyResponse() {
        if(payload == null) return false;
        if(!(payload instanceof JAXBElement)) return false;
        JAXBElement element = (JAXBElement)payload ;
        if(!(element.getValue() instanceof IdentifyResponseType)) return false;
        return true;
    }
    
    public void setPayload(Object jaxb, JAXBContext ctx) {
        payload = jaxb;
        payloadContext = (JAXBRIContext)ctx;
    }
    
    public void setFragmentCreateResponse(DialectableMixedDataType header,
            EndpointReference epr) throws Exception {
        setCreateResponse(epr);
        JAXBElement<DialectableMixedDataType> jaxb =
                Management.FACTORY.createFragmentTransfer(header);
        headers.add(Headers.create(jaxbContext, jaxb));
    }
    
    public void setCreateResponse(EndpointReference epr) throws Exception {
        MemberSubmissionEndpointReference translated;
        if(epr instanceof MemberSubmissionEndpointReference)
            translated = (MemberSubmissionEndpointReference) epr;
        else {
            WSEndpointReference wsepr = new WSEndpointReference(epr);
            translated = wsepr.toSpec(MemberSubmissionEndpointReference.class);
        }
        JAXBElement<MemberSubmissionEndpointReference> je = new JAXBElement<MemberSubmissionEndpointReference>(
               new QName(Transfer.NS_URI, "ResourceCreated"),
                MemberSubmissionEndpointReference.class,translated); 
        payload = je;
        payloadContext = getEPRContext();
    }
    
    public void setAction(String action) throws Exception {
        this.action = action;
    }
    
    public void addExtraHeader(Object obj, JAXBContext ctx) throws Exception {
        if(extraHeaders == null)
            extraHeaders = new HeaderList();
        
        if(obj instanceof Element)
            extraHeaders.add(Headers.create((Element) obj));
        else
            extraHeaders.add(Headers.create((JAXBRIContext)ctx, obj));
    }
    
    public void addExtraHeaders(List<Object> extras, JAXBContext ctx) throws Exception {
        if(extras == null)return;
        for(Object obj : extras) {
            addExtraHeader(obj, ctx);
        }
    }
    
    public void setIdentifier(String id) throws JAXBException, SOAPException{
        headers.add(new StringHeader(Eventing.IDENTIFIER, id));
    }
    
    // XXX REVISIT, WHAT ABPUT MULTIPLE TIME SET HEADERS!
    // Need to protect about that
    public void setTotalItemsCountEstimate(final BigInteger itemCount) throws JAXBException {
        final AttributableNonNegativeInteger count = new AttributableNonNegativeInteger();
        final JAXBElement<AttributableNonNegativeInteger> countElement =
                Management.FACTORY.createTotalItemsCountEstimate(count);
        if (itemCount == null) {
            /*
             * TODO: does not work yet - bug in JAXB 2.0 FCS, see Issue 217 in JAXB
             * https://jaxb.dev.java.net/issues/show_bug.cgi?id=217
             */
            countElement.setNil(true);
        } else {
            count.setValue(itemCount);
        }
        headers.add(new JAXBHeader(jaxbContext, countElement));
    }
    
    public void setEnumerateResponse(final Object context, final String expires, final Object... anys)
    throws JAXBException, SOAPException {
        final EnumerateResponse response = Enumeration.FACTORY.createEnumerateResponse();
        
        final EnumerationContextType contextType = Enumeration.FACTORY.createEnumerationContextType();
        contextType.getContent().add(context);
        response.setEnumerationContext(contextType);
        
        if (expires != null) {
            response.setExpires(expires.trim());
        }
        
        // Check if we have any items to add to the response
        // We will have items if this is an optimized enumeration
        
        if (anys != null) {
            for (final Object any : anys) {
                if (any != null) {
                    response.getAny().add(any);
                }
            }
        }
        payload = response;
    }
    
    public void setEnumerateResponse(final Object context, final String expires,
            final List<EnumerationItem> items, final EnumerationModeType mode, final boolean haveMore)
            throws JAXBException, SOAPException {
        
        final AnyListType anyListType = Management.FACTORY.createAnyListType();
        final List<Object> any = anyListType.getAny();
        if (items != null) {
            for (final EnumerationItem ee : items) {
                MessageUtil.addEnumerationItem(any, ee, mode);
            }
            
            JAXBElement anyList = Management.FACTORY.createItems(anyListType);
            if (!haveMore) {
                JAXBElement<AttributableEmpty> eos = Management.FACTORY
                        .createEndOfSequence(new AttributableEmpty());
                setEnumerateResponse(context, expires, anyList, eos);
            } else {
                setEnumerateResponse(context, expires, anyList);
            }
        } else {
            setEnumerateResponse(context, expires);
        }
    }
    
    public void setPullResponse(final List<EnumerationItem> items, final Object context, final boolean haveMore, EnumerationModeType mode)
    throws JAXBException, SOAPException {
        
        final ItemListType itemList = EnumerationExtensions.FACTORY.createItemListType();
        final List<Object> itemListAny = itemList.getAny();
        // go through each element in the list and add appropriate item to list
        // depending on the EnumerationModeType
        for (final EnumerationItem ee : items) {
            MessageUtil.addEnumerationItem(itemListAny,ee,mode);
        }
        
        setPullResponse(itemList, context, haveMore);
    }
    
    private void setPullResponse(final ItemListType itemList, final Object context, final boolean haveMore)
    throws JAXBException, SOAPException {
        final PullResponse response = Enumeration.FACTORY.createPullResponse();
        
        response.setItems(itemList);
        
        if (haveMore) {
            final EnumerationContextType contextType = Enumeration.FACTORY.createEnumerationContextType();
            contextType.getContent().add(context);
            response.setEnumerationContext(contextType);
        } else {
            response.setEndOfSequence("");
        }
        
        payload = response;
    }
    
    public void setSubscribePullResponse(final EndpointReferenceType mgr, final String expires,
            final Object context)
            throws SOAPException, JAXBException {
        
        final EnumerationContextType contextType = Enumeration.FACTORY.createEnumerationContextType();
        contextType.getContent().add(context);
        // TODO: this should have been generated by JAXB as - createEnumerationContextType(contextType);
        final JAXBElement<EnumerationContextType> contextTypeElement = 
                new JAXBElement<EnumerationContextType>(Enumeration.ENUMERATION_CONTEXT, EnumerationContextType.class, null, contextType);
        setSubscribeResponse(mgr, expires, contextTypeElement);
    }
    
    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires)
            throws SOAPException, JAXBException {
    	setSubscribeResponse(mgr, expires, (Object[])null);
    }

    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires,
            final Object... extensions)
            throws SOAPException, JAXBException {
        final SubscribeResponse response = Eventing.FACTORY.createSubscribeResponse();
        response.setSubscriptionManager(mgr);
        response.setExpires(expires);
        if (extensions != null) {
            for (final Object ext : extensions) {
                response.getAny().add(ext);
            }
        }
        payload = response;
    }
    
    public void setRenewResponse(final String expires) throws SOAPException, JAXBException {
    	setRenewResponse(expires, (Object[])null);
    }
    
    public void setRenewResponse(final String expires,
    		final Object... extensions) throws SOAPException, JAXBException {
        final RenewResponse response = Eventing.FACTORY.createRenewResponse();
        response.setExpires(expires);
        if (extensions != null) {
            for (final Object ext : extensions) {
                response.getAny().add(ext);
            }
        }
        payload = response;
    }
    
    public void setSubscriptionManagerEpr(EndpointReferenceType mgr) throws JAXBException, SOAPException {
        SubscribeResponse response = (SubscribeResponse) payload;
        if (response == null)
            this.setSubscribeResponse(mgr, null);
        else
            response.setSubscriptionManager(mgr);
    }
    
    public void addNamespaceDeclarations(Map<String, String> declarations) {
        // XXX REVISIT TODO
    }

    public SOAPMessage toSOAPMessage() throws Exception {
        return buildMessage().readAsSOAPMessage();
    }
}
