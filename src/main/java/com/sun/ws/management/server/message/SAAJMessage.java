/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 * 
 ** Copyright (C) 2006-2008 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.Management;
import com.sun.ws.management.MessageUtil;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.transfer.TransferExtensions;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Headers;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.EndpointReference;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.Locale;
import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionSet;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;

import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;
import org.xmlsoap.schemas.ws._2004._08.eventing.Renew;

/**
 *
 * @author jfdenise
 */
public class SAAJMessage implements WSManagementRequest, WSManagementResponse {
    private Management mgt;
    
    public SAAJMessage(Management mgt) {
        this.mgt = mgt;
    }
    
    public Object getPayload(Unmarshaller u) throws Exception {
        SOAPElement[] child = mgt.getChildren(mgt.getBody());
        if(child == null)
            return null;
        Unmarshaller current = u == null ? mgt.getXmlBinding().createUnmarshaller() : u;
        Object obj = child[0];
        try {
            obj =current.unmarshal(child[0]);
        }catch(JAXBException ex) {
            // OK will return DOM
        }
        return obj;
    }
    public List<Header> getSOAPHeaders() throws Exception {
        SOAPElement[] elements = mgt.getChildren(mgt.getHeader());
        List<Header> list = new ArrayList<Header>();
        for (SOAPElement se : elements) {
            try {
                list.add(Headers.create((SOAPHeaderElement)se));
            }catch(Exception ex) {
                // We can't add the element.'
            }
        }
        return list;
    }
    
    public SOAPMessage toSOAPMessage() throws Exception {
        return mgt.getMessage();
    }
    
    public URI getAddressURI() throws SOAPException, JAXBException, URISyntaxException {
        return MessageUtil.checkAddressURI(mgt.getTo());
    }
    
    public URI getActionURI() throws SOAPException, JAXBException, URISyntaxException {
        return new URI(mgt.getAction());
    }
    
    public Duration getTimeout() throws JAXBException, SOAPException {
        return mgt.getTimeout();
    }
    
    public Set<SelectorType> getSelectors() throws JAXBException, SOAPException {
        return mgt.getSelectors();
    }
    
    public OptionSet getOptionSet() throws JAXBException, SOAPException {
        SOAPElement[] elements = mgt.getChildren(mgt.getHeader());
        for (SOAPElement header : elements) {
            if(header.getLocalName().equals(Management.OPTION_SET.getLocalPart()) 
                    && header.getNamespaceURI().equals(Management.OPTION_SET.getNamespaceURI())) {
               OptionSet os = (OptionSet) mgt.getXmlBinding().unmarshal(header);
               return os;
            }
        }
        return null;
    }
    
    public MaxEnvelopeSizeType getMaxEnvelopeSize() throws JAXBException, SOAPException {
        return mgt.getMaxEnvelopeSize();
    }
    
    public Locale getLocale() throws JAXBException, SOAPException {
        return mgt.getLocale();
    }
    
    public boolean isIdentify() throws JAXBException, SOAPException {
        Identify i = new Identify(mgt);
        return i.getIdentify() != null;
    }
    
    public Renew getRenew() throws JAXBException, SOAPException {
        Eventing evt = new  Eventing(mgt);
        return evt.getRenew();
    }
    
    public String getIdentifier() throws JAXBException, SOAPException {
        Eventing evt = new Eventing(mgt);
        return evt.getIdentifier();
    }
    
    public Subscribe getSubscribe() throws JAXBException, SOAPException {
        Eventing evt = new Eventing(mgt);
        return evt.getSubscribe();
    }
    
    public String getResourceURIForEventing() throws JAXBException, SOAPException {
        return mgt.getResourceURI();
    }
    
    public Unsubscribe getUnsubscribe() throws JAXBException, SOAPException {
        Eventing evt = new Eventing(mgt);
        return evt.getUnsubscribe();
    }
    
    public DialectableMixedDataType getFragmentHeaderContent() throws Exception {
        TransferExtensions tx = new TransferExtensions(mgt);
        SOAPHeaderElement el = tx.getFragmentHeader();
        JAXBElement<DialectableMixedDataType> jaxb =
                (JAXBElement<DialectableMixedDataType>)mgt.getXmlBinding().unmarshal(el);
        return jaxb.getValue();
    }
    
    public Enumerate getEnumerate() throws JAXBException, SOAPException {
        Enumeration en = new Enumeration(mgt);
        return en.getEnumerate();
    }
    
    public EnumerationExtensions.Mode getMode() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getMode();
    }
    
    public EnumerationModeType getModeType() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getModeType();
    }
    
    public boolean getOptimize() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getOptimize();
    }
    
    public int getMaxElements() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getMaxElements();
    }
    
    public Duration getMaxTime() throws JAXBException, SOAPException {
        if(mgt.getAction().equals(Enumeration.PULL_ACTION_URI))
            return getTimeout();
        else
            return null;
    }
    
    public AttributableEmpty getRequestTotalItemsCountEstimate() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getRequestTotalItemsCountEstimate();
    }
    
    public Pull getPull() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getPull();
    }
    
    public Release getRelease() throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        return en.getRelease();
    }
    
    public void setIdentifyResponse(String vendor, String productVersion, String protocolVersion, Map<QName, String> more) throws Exception {
        Identify i = new Identify(mgt);
        i.setIdentifyResponse(vendor, productVersion, protocolVersion, more);
    }
    
    public void setSubscribePullResponse(final EndpointReferenceType mgr, final String expires, final Object context) throws SOAPException, JAXBException {
        EventingExtensions evt = new EventingExtensions(mgt);
        evt.setSubscribeResponse(mgr, expires,context);
    }
    
    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires) throws SOAPException, JAXBException {
    	setSubscribeResponse(mgr, expires, (Object[])null);
    }
    
    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires, final Object... extensions) throws SOAPException, JAXBException {
        Eventing evt = new Eventing(mgt);
        evt.setSubscribeResponse(mgr, expires, extensions);
    }
    
    public void setRenewResponse(final String expires) throws SOAPException, JAXBException {
        setRenewResponse(expires, (Object[])null);
    }
    
    public void setRenewResponse(final String expires, final Object... extensions) throws SOAPException, JAXBException {
        Eventing evt = new Eventing(mgt);
        evt.setRenewResponse(expires, extensions);
    }
    
    public void setSubscriptionManagerEpr(EndpointReferenceType mgr) throws JAXBException, SOAPException {
        Eventing evt = new Eventing(mgt);
        evt.setSubscriptionManagerEpr(mgr);
    }
    
    public void setCreateResponse(EndpointReference epr) throws Exception {
        TransferExtensions tr = new TransferExtensions(mgt);
        EndpointReferenceType eprType = MessageUtil.toEPRType(mgt.getXmlBinding().getJAXBContext(),
                epr);
        tr.setCreateResponse(eprType);
    }
    
    public void setFragmentCreateResponse(DialectableMixedDataType header, EndpointReference epr) throws Exception {
        setCreateResponse(epr);
        JAXBElement<DialectableMixedDataType> jaxb =
                Management.FACTORY.createFragmentTransfer(header);
        mgt.getXmlBinding().marshal(jaxb, mgt.getHeader());
    }
    
    public void setTotalItemsCountEstimate(final BigInteger itemCount) throws JAXBException {
        try {
            EnumerationExtensions en = new EnumerationExtensions(mgt);
            en.setTotalItemsCountEstimate(itemCount);
        }catch(SOAPException s) {
            throw new JAXBException(s);
        }
    }
    
    public void setEnumerateResponse(final Object context, final String expires, final Object... anys) throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        en.setEnumerateResponse(context, expires, anys);
    }
    
    public void setEnumerateResponse(final Object context, final String expires, final List<EnumerationItem> items, final EnumerationModeType mode, final boolean haveMore) throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        en.setEnumerateResponse(context, expires, items, mode, haveMore);
    }
    
    public void setPullResponse(final List<EnumerationItem> items, final Object context, final boolean haveMore, EnumerationModeType mode) throws JAXBException, SOAPException {
        EnumerationExtensions en = new EnumerationExtensions(mgt);
        en.setPullResponse(items, context, haveMore, mode);
    }
    
    public URI getResourceUri() throws SOAPException, JAXBException, FaultException {
        return MessageUtil.checkResourceURI(mgt.getResourceURI());
    }
    
    public String getResourceURIForEnumeration() throws JAXBException, SOAPException {
        return mgt.getResourceURI();
    }
    
    public DialectableMixedDataType getWsmanEventingFilter() throws JAXBException, SOAPException {
        return new EventingExtensions(mgt).getWsmanFilter();
    }
    
    public org.xmlsoap.schemas.ws._2004._08.eventing.FilterType getEventingFilter() throws JAXBException, SOAPException {
        return new Eventing(mgt).getSubscribe().getFilter();
    }
    
    public DialectableMixedDataType getWsmanEnumerationFilter() throws JAXBException, SOAPException {
        return new EnumerationExtensions(mgt).getWsmanFilter();
    }
    
    public FilterType getEnumerationFilter() throws JAXBException, SOAPException {
        return new Enumeration(mgt).getFilter();
    }
    
    public NamespaceContext getNamespaceContext() throws Exception {
        return MessageUtil.getNamespaceContext(this);
    }
    
    public void setFragmentResponse(DialectableMixedDataType header,
            List<Object> values, JAXBContext context) throws Exception {
        if(values != null) {
            JAXBElement<MixedDataType> mixed =
                    MessageUtil.buildXmlFragment(values);
            mgt.getXmlBinding().marshal(mixed, mgt.getBody());
        }
        
        JAXBElement<DialectableMixedDataType> jaxb =
                Management.FACTORY.createFragmentTransfer(header);
        mgt.getXmlBinding().marshal(jaxb, mgt.getHeader());
    }
    
    public void validate() throws SOAPException, JAXBException, FaultException {
        mgt.validate();
    }
    
    public void setAction(String actionURI) throws Exception {
        mgt.setAction(actionURI);
    }
    
    public void addNamespaceDeclarations(Map<String, String> declarations) throws SOAPException {
        mgt.addNamespaceDeclarations(declarations);
    }
    
    public void setFault(FaultException f) throws Exception {
        mgt.setFault(f);
    }
    
    public boolean containsFault() throws Exception {
        return mgt.getFault() != null;
    }
    
    public JAXBContext getJAXBContext() {
        return mgt.getXmlBinding().getJAXBContext();
    }
    
    public void addExtraHeaders(List<Object> list, JAXBContext ctx) throws Exception {
        SOAPHeader headers = mgt.getHeader();
        // JAXBContext current = ctx == null ? mgt.getXmlBinding().getJAXBContext() : ctx;
        for(Object obj : list) {
            if(obj instanceof Element)
                headers.appendChild((Element) obj);
            else
                ctx.createMarshaller().marshal(obj, headers);
        }
    }
    
    public void setPayload(Object jaxb, JAXBContext ctx) throws Exception {
        // JAXBContext current = ctx == null ? mgt.getXmlBinding().getJAXBContext() : ctx;
        ctx.createMarshaller().marshal(jaxb, mgt.getBody());
    }
    /*
    public void setMessageStatus(final WSMessageStatus status ) {
        this.mgt.setMessageStatus(status);
    }*/
    
	public void cancel() throws IllegalStateException {
        this.mgt.getMessageStatus().cancel();
	}

	public void commit() throws IllegalStateException {
		this.mgt.getMessageStatus().commit();		
	}

	public boolean isCanceled() {
		return this.mgt.getMessageStatus().isCanceled();
	}

	public boolean isCommitted() {
		return this.mgt.getMessageStatus().isCommitted();
	}
}
