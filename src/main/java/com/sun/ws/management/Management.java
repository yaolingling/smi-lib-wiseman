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
 **Revision 1.11  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 **
 * $Id: Management.java,v 1.12 2007-11-30 14:32:37 denis_rachal Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.Enumeration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableDuration;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.Locale;
import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionSet;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;

public class Management extends Addressing {

    public static final String NS_PREFIX = "wsman";
    public static final String NS_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";

    public static final String EVENTS_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Events";
    public static final String HEARTBEAT_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Heartbeat";
    public static final String DROPPED_EVENTS_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/DroppedEvents";
    public static final String ACK_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Ack";
    public static final String EVENT_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Event";
    public static final String BOOKMARK_EARLIEST_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/bookmark/earliest";
    public static final String PUSH_WITH_ACK_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/PushWithAck";
    public static final String PULL_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Pull";
    public static final String FAULT_ACTION_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/fault";

    public static final QName RESOURCE_URI = new QName(NS_URI, "ResourceURI", NS_PREFIX);
    public static final QName OPERATION_TIMEOUT = new QName(NS_URI, "OperationTimeout", NS_PREFIX);
    public static final QName SELECTOR_SET = new QName(NS_URI, "SelectorSet", NS_PREFIX);
    public static final QName OPTION_SET = new QName(NS_URI, "OptionSet", NS_PREFIX);
    public static final QName MAX_ENVELOPE_SIZE = new QName(NS_URI, "MaxEnvelopeSize", NS_PREFIX);
    public static final QName LOCALE = new QName(NS_URI, "Locale", NS_PREFIX);
    public static final QName FAULT_DETAIL = new QName(NS_URI, "FaultDetail", NS_PREFIX);
    public static final QName URL = new QName(NS_URI, "URL", NS_PREFIX);
    public static final QName ENDPOINT_REFERENCE = new QName(NS_URI, "EndpointReference", NS_PREFIX);

    public static final ObjectFactory FACTORY = new ObjectFactory();

    public Management() throws SOAPException {
        super();
    }

    public Management(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public Management(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public Management(final SOAPMessage msg) throws SOAPException {
        super(msg);
    }

    // setters

    public void setResourceURI(final String resource) throws JAXBException, SOAPException {
        removeChildren(getHeader(), RESOURCE_URI);
        final AttributableURI resType = FACTORY.createAttributableURI();
        resType.setValue(resource);
        final JAXBElement<AttributableURI> resTypeElement = FACTORY.createResourceURI(resType);
        getXmlBinding().marshal(resTypeElement, getHeader());
    }

    public void setTimeout(final Duration duration) throws JAXBException, SOAPException {
        removeChildren(getHeader(), OPERATION_TIMEOUT);
        final AttributableDuration durationType = FACTORY.createAttributableDuration();
        durationType.setValue(duration);
        final JAXBElement<AttributableDuration> durationElement = FACTORY.createOperationTimeout(durationType);
        getXmlBinding().marshal(durationElement, getHeader());
    }

    public void setSelectors(final Set<SelectorType> selectors) throws JAXBException, SOAPException {
        removeChildren(getHeader(), SELECTOR_SET);
        final SelectorSetType selectorSet = FACTORY.createSelectorSetType();
        final Iterator<SelectorType> si = selectors.iterator();
        while (si.hasNext()) {
            selectorSet.getSelector().add(si.next());
        }
        final JAXBElement<SelectorSetType> selectorSetElement = FACTORY.createSelectorSet(selectorSet);
        getXmlBinding().marshal(selectorSetElement, getHeader());
    }

    public void setMaxEnvelopeSize(final MaxEnvelopeSizeType size) throws JAXBException, SOAPException {
        removeChildren(getHeader(), MAX_ENVELOPE_SIZE);
        final JAXBElement<MaxEnvelopeSizeType> sizeElement = FACTORY.createMaxEnvelopeSize(size);
        getXmlBinding().marshal(sizeElement, getHeader());
    }

    public void setLocale(final Locale locale) throws JAXBException, SOAPException {
        removeChildren(getHeader(), LOCALE);
        getXmlBinding().marshal(locale, getHeader());
    }

    public void setOptions(final Set<OptionType> options) throws JAXBException, SOAPException {
        removeChildren(getHeader(), OPTION_SET);
        final OptionSet optionSet = FACTORY.createOptionSet();
        final Iterator<OptionType> oi = options.iterator();
        while (oi.hasNext()) {
            optionSet.getOption().add(oi.next());
        }
        getXmlBinding().marshal(optionSet, getHeader());
    }

    // getters

    public String getResourceURI() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), RESOURCE_URI);
        return value == null ? null : ((JAXBElement<AttributableURI>) value).getValue().getValue();
    }

    public Duration getTimeout() throws JAXBException, SOAPException {
    	    Duration result = null;
    		final Object value = unbind(getHeader(), OPERATION_TIMEOUT);
    		if (value != null) {
    	        result = ((JAXBElement<AttributableDuration>) value).getValue().getValue();
    		} else {
    			// wsman:OperationTimeout no set.
    			// Check if this is a wsen:Pull & wsen:MaxTime was set.
    			final Enumeration enu = new Enumeration(this);
    			final Pull pull = enu.getPull();
    			if (pull != null) {
    				result = pull.getMaxTime();
    			}
    		}
            return result;
    }

    public Set<SelectorType> getSelectors() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), SELECTOR_SET);
        return value == null ? null : new HashSet<SelectorType>(((JAXBElement<SelectorSetType>) value).getValue().getSelector());
    }

    public Set<OptionType> getOptions() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), OPTION_SET);
        return value == null ? null : new HashSet<OptionType>(((OptionSet) value).getOption());
    }

    public MaxEnvelopeSizeType getMaxEnvelopeSize() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), MAX_ENVELOPE_SIZE);
        return value == null ? null : ((JAXBElement<MaxEnvelopeSizeType>) value).getValue();
    }

    public Locale getLocale() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), LOCALE);
        return (Locale) value;
    }
}

