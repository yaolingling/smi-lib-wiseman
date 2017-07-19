/*
 * Copyright 2006-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 * 
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 */

package com.sun.ws.management.server.message;

import com.sun.ws.management.InvalidSelectorsFault;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableDuration;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributablePositiveInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.Locale;
import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionSet;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Renew;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

import com.sun.ws.management.Management;
import com.sun.ws.management.MessageUtil;
import com.sun.ws.management.addressing.InvalidMessageInformationHeaderFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingExtensions;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.TransferExtensions;
import com.sun.ws.management.xml.XmlBinding;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
/**
 *
 * @author jfdenise
 */
public class JAXWSMessageRequest implements WSManagementRequest {
    
    private Message message;
    private Message copied;
    private HeaderList headers;
    // private boolean headersRead;
    // private List<Object> stdHeaders;
    private XmlBinding binding;
    private static DatatypeFactory datatypeFactory;
    private boolean maxEnvelopeSizeRead;
    private MaxEnvelopeSizeType maxEnvelopeSize;
    private boolean timeoutRead;
    private Duration timeout;
    private boolean isIdentifyRead;
    private boolean isIdentify;
    private boolean resourceURIRead;
    private URI resourceURI;
    private boolean addressURIRead;
    private URI addressURI;
    private OptionSet options;
    private Locale locale;
    private boolean optionsRead;
    private boolean localeRead;
    private String action;
    private URI actionURI;
    private boolean renewRead;
    private Renew renew;
    private boolean renewIdentifierRead;
    private String renewIdentifier;
    private boolean selectorsRead;
    private Set<SelectorType> selectors;
    private boolean fragHeaderRead;
    private DialectableMixedDataType fragHeader;
    private boolean enumerateRead;
    private Enumerate enumerate;
    private boolean reqTotalCountEstimateRead;
    private AttributableEmpty reqTotalCountEstimate;
    private boolean pullRead;
    private Pull pull;
    private boolean releaseRead;
    private Release release;
    private boolean subscribeRead;
    private Subscribe subscribe;
    private boolean unsubscribeRead;
    private Unsubscribe unsubscribe;
    private boolean evtWsmanFilterRead;
    private DialectableMixedDataType evtWsmanFilter;
    private boolean evtFilterRead;
    private org.xmlsoap.schemas.ws._2004._08.eventing.FilterType evtFilter;
    private boolean enWsmanFilterRead;
    private DialectableMixedDataType enWsmanFilter;
    private boolean enFilterRead;
    private FilterType enFilter;
    // private boolean nsContextRead;
    // private NamespaceContext nsContext;
    private boolean payloadRead;
    private Object payload;
    private boolean soapMessageRead;
    private SOAPMessage soapMessage;
    private WSMessageStatus status;
    
    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        }catch(Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }
    /** Creates a new instance of WSMessage */
    public JAXWSMessageRequest(Message message, XmlBinding binding) {
        this.message = message;
        // XXX REVISIT, WE NEED THIS COPY IN CASE toSOAPMessage is called!!!!
        this.copied = message.copy();
        this.headers = message.getHeaders();
        this.binding = binding;
        this.status = new WSMessageStatus();
    }
    
    private Unmarshaller newUnmarshaller() throws JAXBException {
        return binding.createUnmarshaller();
    }
    
    public void validate() throws SOAPException, JAXBException, FaultException {
        
    }
    
    public Duration getTimeout() throws JAXBException, SOAPException {
        if(!timeoutRead) {
            timeoutRead = true;
            Header h = headers.get(Management.OPERATION_TIMEOUT, true);
            if(h != null) {
                try {
                    Object value = h.readAsJAXB(newUnmarshaller());
                    timeout = value == null ? null : ((JAXBElement<AttributableDuration>) value).getValue().getValue();
                }catch(JAXBException ex) {
                    throw new InvalidMessageInformationHeaderFault("wsman:OperationTimeout");
                }
            } else {
            	Duration result = null;
            	
            	// Check if this is a wsen:Pull & wsen:MaxTime is set.
                String action = getAction();
                if(action != null && action.equals(Enumeration.PULL_ACTION_URI)) {
                    final Pull pull = getPull();
                    if (pull != null)
                        result = pull.getMaxTime();
                }
                timeout = result;
            }
        }
        
        return timeout;
    }
    
    public MaxEnvelopeSizeType getMaxEnvelopeSize() throws JAXBException, SOAPException {
        if(!maxEnvelopeSizeRead) {
            maxEnvelopeSizeRead = true;
            Header h = headers.get(Management.MAX_ENVELOPE_SIZE, true);
            if(h != null) {
                final Object value = h.readAsJAXB(newUnmarshaller());
                maxEnvelopeSize = value == null ? null : ((JAXBElement<MaxEnvelopeSizeType>) value).getValue();
            }
        }
        return maxEnvelopeSize;
    }
    
    public boolean isIdentify() throws JAXBException, SOAPException {
        if(!isIdentifyRead) {
            isIdentifyRead = true;
            isIdentify = Identify.IDENTIFY.getNamespaceURI().equals(message.getPayloadNamespaceURI()) &&
                    Identify.IDENTIFY.getLocalPart().equals(message.getPayloadLocalPart());
        }
        return isIdentify;
    }
    
    public URI getResourceUri() throws JAXBException, SOAPException, FaultException {
        if(!resourceURIRead) {
            resourceURIRead = true;
            Header h = headers.get(Management.RESOURCE_URI, true);
            String resURI = h == null ? null : h.getStringContent();
            resourceURI = MessageUtil.checkResourceURI(resURI);
        }
        return resourceURI;
    }
    
    public URI getAddressURI() throws JAXBException, SOAPException, FaultException {
        if(!addressURIRead) {
            addressURIRead = true;
            String to = null;
            try {
                to = headers.getTo(AddressingVersion.MEMBER,  SOAPVersion.SOAP_12);
            }catch(Exception ex) {
                to = null;
            }
            addressURI = MessageUtil.checkAddressURI(to);
        }
        return addressURI;
    }
    
    public OptionSet getOptionSet() throws JAXBException, SOAPException {
        if(!optionsRead) {
            optionsRead = true;
            Header h = headers.get(Management.OPTION_SET, true);
            Object value = null;
            if(h != null)
                value = h.readAsJAXB(newUnmarshaller());
            if(value != null) {
                OptionSet orig = (OptionSet) value;
                options = new OptionSet();
                options.getOtherAttributes().putAll(orig.getOtherAttributes());
                options.getOption().addAll(orig.getOption());
            }
        }
        return options;
    }
    
    public Locale getLocale() throws JAXBException, SOAPException {
        if(!localeRead) {
            localeRead = true;
            Header h = headers.get(Management.LOCALE, true);
            if(h != null)
                locale = (Locale) h.readAsJAXB(newUnmarshaller());
        }
        
        return locale;
    }
    
    /**
     *
     * Direct access to header data structure.
     * Used to provide an access to unknown headers.
     * Workaround more than design.
     */
    public List<Header> getSOAPHeaders() {
        return headers;
    }
    
    public String getAction() throws JAXBException, SOAPException {
        if(action == null) {
            action = headers.getAction(AddressingVersion.MEMBER,  SOAPVersion.SOAP_12);
        }
        return action;
    }
    
    public Set<SelectorType> getSelectors() throws Exception {
        if(!selectorsRead) {
            selectorsRead = true;
            Header h = headers.get(Management.SELECTOR_SET, true);
            if(h != null) {
                try{
                    Object selectorSet = h.readAsJAXB(newUnmarshaller());
                    selectors = (selectorSet == null) ? null :
                        new HashSet<SelectorType>(((JAXBElement<SelectorSetType>) selectorSet).getValue().getSelector());
                }catch(JAXBException ex) {
                    throw new InvalidSelectorsFault(InvalidSelectorsFault.
                            Detail.INVALID_VALUE);
                }
            }
        }
        return selectors;
    }
    
    public Renew getRenew() throws JAXBException, SOAPException{
        if(!renewRead) {
            renewRead = true;
            renew = (Renew) extractPayload();
        }
        return renew;
    }
    
    public String getIdentifier() throws JAXBException, SOAPException{
        if(!renewIdentifierRead) {
            renewIdentifierRead = true;
            Header h = headers.get(Eventing.IDENTIFIER, true);
            if(h != null)
                renewIdentifier = ((JAXBElement<String>) h.readAsJAXB(newUnmarshaller())).getValue();
        }
        return renewIdentifier;
    }
    
    public DialectableMixedDataType getFragmentHeaderContent() throws Exception {
        if(!fragHeaderRead) {
            fragHeaderRead = true;
            Header h = headers.get(TransferExtensions.FRAGMENT_TRANSFER, true);
            if(h != null)   {
                JAXBElement<DialectableMixedDataType> jaxb =
                        (JAXBElement<DialectableMixedDataType>) h.readAsJAXB(newUnmarshaller());
                fragHeader = jaxb.getValue();
            }
        }
        return fragHeader;
    }
    
    public FilterType getFilter() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return null;
        }
        return enumerate.getFilter();
    }
    
    public Enumerate getEnumerate() throws JAXBException, SOAPException {
        if(!enumerateRead) {
            enumerateRead = true;
            enumerate = (Enumerate) extractPayload();
        }
        return enumerate;
    }
    
    public DialectableMixedDataType getWsmanFilter() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return null;
        }
        return (DialectableMixedDataType) extract(enumerate.getAny(),
                DialectableMixedDataType.class,
                EnumerationExtensions.FILTER);
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
    
    public EnumerationModeType getModeType() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return null;
        }
        EnumerationModeType type =  (EnumerationModeType) extract(enumerate.getAny(),
                EnumerationModeType.class,
                EnumerationExtensions.ENUMERATION_MODE);
        return (type == null) ? null : type;
    }
    
    public EnumerationExtensions.Mode getMode() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return null;
        }
        EnumerationModeType type =  getModeType();
        return (type == null) ? null : EnumerationExtensions.Mode.valueOf(type.value());
    }
    
    public boolean getOptimize() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return false;
        }
        AttributableEmpty optimize =  (AttributableEmpty) extract(enumerate.getAny(),
                AttributableEmpty.class,
                EnumerationExtensions.OPTIMIZE_ENUMERATION);
        return (optimize == null) ? false : true;
    }
    
    public int getMaxElements() throws JAXBException, SOAPException {
        Enumerate enumerate = getEnumerate();
        
        if (enumerate == null) {
            return 1;
        }
        AttributablePositiveInteger max =  (AttributablePositiveInteger) extract(enumerate.getAny(),
                AttributablePositiveInteger.class,
                EnumerationExtensions.MAX_ELEMENTS);
        
        return (max == null) ? 1 : max.getValue().intValue();
    }
    
    public Duration getMaxTime() throws JAXBException, SOAPException {
        if(getAction().equals(Enumeration.PULL_ACTION_URI))
            return getTimeout();
        else
            return null;
    }
    
    public SOAPMessage toSOAPMessage() throws Exception {
        if(!soapMessageRead) {
            soapMessageRead = true;
            if(copied == null)
                copied = message.copy();
            soapMessage = copied.readAsSOAPMessage();
        }
        return soapMessage;
    }
    
    public NamespaceContext getNamespaceContext() throws Exception {
        return MessageUtil.getNamespaceContext(this);
    }
    
    public Object getPayload(Unmarshaller u) throws Exception {
        if(!payloadRead) {
            payloadRead = true;
            Unmarshaller current = u == null ? binding.createUnmarshaller() : u;
            try {
                payload = extractPayload(current);
            }catch(Exception ex) {
                // OK will return XMLStreamReader
                Message safe = copied.copy();
                payload = copied.readPayload();
                copied = safe;
            }
        }
        return payload;
    }
    
    public AttributableEmpty getRequestTotalItemsCountEstimate() throws JAXBException, SOAPException {
        if(!reqTotalCountEstimateRead) {
            reqTotalCountEstimateRead = true;
            Header h = headers.get(EnumerationExtensions.REQUEST_TOTAL_ITEMS_COUNT_ESTIMATE, true);
            if(h != null)   {
                JAXBElement<AttributableEmpty> jaxb =
                        (JAXBElement<AttributableEmpty>) h.readAsJAXB(newUnmarshaller());
                reqTotalCountEstimate = jaxb.getValue();
            }
        }
        return reqTotalCountEstimate;
    }
    
    public Pull getPull() throws JAXBException, SOAPException {
        if(!pullRead) {
            pullRead = true;
            pull = (Pull) extractPayload();
        }
        return pull;
    }
    
    private Object extractPayload(Unmarshaller u) throws JAXBException {
        Object ret;
        if(copied == null)
            copied = message.copy();
        ret = message.readPayloadAsJAXB(u);
        return ret;
    }
    
    private Object extractPayload() throws JAXBException {
        return extractPayload(newUnmarshaller());
    }
    
    public Release getRelease() throws JAXBException, SOAPException {
        if(!releaseRead) {
            releaseRead = true;
            release = (Release) extractPayload();
        }
        return release;
    }
    
    public String getResourceURIForEventing() throws JAXBException, SOAPException {
        try {
            return getResourceUri().toString();
        } catch (Exception ex) {
            if(ex instanceof SOAPException) {
                throw (SOAPException)ex;
            }
            if(ex instanceof JAXBException) {
                throw (JAXBException)ex;
            }
            throw new RuntimeException(ex.toString());
        }
    }
    
    public Subscribe getSubscribe() throws JAXBException, SOAPException {
        if(!subscribeRead) {
            subscribeRead = true;
            subscribe = (Subscribe) extractPayload();
        }
        return subscribe;
    }
    
    public Unsubscribe getUnsubscribe() throws JAXBException, SOAPException {
        if(!unsubscribeRead) {
            unsubscribeRead = true;
            unsubscribe = (Unsubscribe) extractPayload();
        }
        return unsubscribe;
    }
    
    public URI getActionURI() throws SOAPException, JAXBException, URISyntaxException {
        if(actionURI == null) {
            actionURI = new URI(getAction());
        }
        return actionURI;
    }
    
    public String getResourceURIForEnumeration() throws JAXBException, SOAPException {
        return getResourceURIForEventing();
    }
    
    public DialectableMixedDataType getWsmanEventingFilter() throws JAXBException, SOAPException {
        if(!evtWsmanFilterRead) {
            evtWsmanFilterRead = true;
            Subscribe subscribe = getSubscribe();
            
            if (subscribe == null) {
                return null;
            }
            evtWsmanFilter = (DialectableMixedDataType) extract(subscribe.getAny(),
                    DialectableMixedDataType.class,
                    EventingExtensions.FILTER);
        }
        return evtWsmanFilter;
    }
    
    public org.xmlsoap.schemas.ws._2004._08.eventing.FilterType getEventingFilter() throws JAXBException, SOAPException {
        if(!evtFilterRead) {
            evtFilterRead = true;
            evtFilter = getSubscribe().getFilter();
        }
        return evtFilter;
    }
    
    public DialectableMixedDataType getWsmanEnumerationFilter() throws JAXBException, SOAPException {
        if(!enWsmanFilterRead) {
            enWsmanFilterRead = true;
            Enumerate enumerate = getEnumerate();
            
            if (enumerate == null) {
                return null;
            }
            enWsmanFilter = (DialectableMixedDataType) extract(enumerate.getAny(),
                    DialectableMixedDataType.class,
                    EnumerationExtensions.FILTER);
        }
        return enWsmanFilter;
    }
    
    public FilterType getEnumerationFilter() throws JAXBException, SOAPException {
        if(!enFilterRead) {
            enFilterRead = true;
            enFilter = getEnumerate().getFilter();
        }
        return enFilter;
    }

    public void setMessageStatus(final WSMessageStatus status ) {
        this.status = status;
    }
    
	public void cancel() throws IllegalStateException {
        this.status.cancel();
	}

	public void commit() throws IllegalStateException {
		this.status.commit();		
	}

	public boolean isCanceled() {
		return this.status.isCanceled();
	}

	public boolean isCommitted() {
		return this.status.isCommitted();
	}
}
