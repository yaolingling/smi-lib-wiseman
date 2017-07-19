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
 **
 * $Id: EventingExtensions.java,v 1.7 2007-05-30 20:31:05 nbeers Exp $
 */

package com.sun.ws.management.eventing;

import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.Enumeration;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableAny;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableDuration;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributablePositiveInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.eventing.FilterType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.dmtf.schemas.wbem.wsman._1.wsman.ConnectionRetryType;
import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.ObjectFactory;

public class EventingExtensions extends Eventing {

    public static final String EVENT_ACTION_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Event";
    public static final String HEARTBEAT_ACTION_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Heartbeat";
    public static final String ACK_ACTION_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Ack";
    public static final String DROPPED_EVENTS_ACTION_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/DroppedEvents";

    public static final String PUSH_WITH_ACK_DELIVERY_MODE = "http://schemas.dmtf.org/wbem/wsman/1/wsman/PushWithAck";
    public static final String EVENTS_DELIVERY_MODE = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Events";
    public static final String PULL_DELIVERY_MODE = "http://schemas.dmtf.org/wbem/wsman/1/wsman/Pull";

    public static final String EARLIEST_BOOKMARK = "http://schemas.dmtf.org/wbem/wsman/1/wsman/bookmark/earliest";

    public static final String CANCEL_SUBSCRIPTION_POLICY = "CancelSubscription";
    public static final String SKIP_POLICY = "Skip";
    public static final String NOTIFY_POLICY = "Notify";

    public static final QName CONNECTION_RETRY = new QName(Management.NS_URI, "ConnectionRetry", Management.NS_PREFIX);
    public static final QName HEARTBEATS = new QName(Management.NS_URI, "Heartbeats", Management.NS_PREFIX);
    public static final QName SEND_BOOKMARKS = new QName(Management.NS_URI, "SendBookmarks", Management.NS_PREFIX);
    public static final QName BOOKMARK = new QName(Management.NS_URI, "Bookmark", Management.NS_PREFIX);
    public static final QName MAX_ELEMENTS = new QName(Management.NS_URI, "MaxElements", Management.NS_PREFIX);
    public static final QName MAX_TIME = new QName(Management.NS_URI, "MaxTime", Management.NS_PREFIX);
    public static final QName EVENTS = new QName(Management.NS_URI, "Events", Management.NS_PREFIX);
    public static final QName EVENT = new QName(Management.NS_URI, "Event", Management.NS_PREFIX);
    public static final QName ACTION = new QName(Management.NS_URI, "Action", Management.NS_PREFIX);
    public static final QName ACK_REQUESTED = new QName(Management.NS_URI, "AckRequested", Management.NS_PREFIX);
    public static final QName DROPPED_EVENTS = new QName(Management.NS_URI, "DroppedEvents", Management.NS_PREFIX);
    public static final QName FILTER = new QName(Management.NS_URI, "Filter", Management.NS_PREFIX);

    public static final ObjectFactory FACTORY = new ObjectFactory();

    public EventingExtensions() throws SOAPException, JAXBException {
        super();
    }

    public EventingExtensions(final Addressing addr) throws SOAPException, JAXBException {
        super(addr);
    }

    public EventingExtensions(final InputStream is) throws SOAPException, JAXBException, IOException {
        super(is);
    }

    public void setSubscribe(final EndpointReferenceType endTo, final String deliveryMode,
            final EndpointReferenceType notifyTo, final String expires, final FilterType filter,
            final ConnectionRetryType retryType, final Duration heartbeats, final Boolean sendBookmarks,
            final AttributableAny bookmark,
            final MaxEnvelopeSizeType maxEnvelopeSize, final Long maxElements, final Duration maxTime)
            throws SOAPException, JAXBException {

        JAXBElement<ConnectionRetryType> retryElement = null;
        if (retryType != null) {
            retryElement = FACTORY.createConnectionRetry(retryType);
        }

        JAXBElement<AttributableDuration> heartbeatsElement = null;
        if (heartbeats != null) {
            final AttributableDuration heartbeatDuration = FACTORY.createAttributableDuration();
            heartbeatDuration.setValue(heartbeats);
            heartbeatsElement = FACTORY.createHeartbeats(heartbeatDuration);
        }

        JAXBElement<AttributableEmpty> sendBookmarksElement = null;
        if (sendBookmarks != null && sendBookmarks.booleanValue()) {
        	sendBookmarksElement = FACTORY.createSendBookmarks(new AttributableEmpty());

        }

        JAXBElement<AttributableAny> bookmarkElement = null;
        if (bookmark != null) {
        	bookmarkElement = FACTORY.createBookmark(bookmark);
        }

        JAXBElement<MaxEnvelopeSizeType> maxEnvelopeSizeElement = null;
        if (maxEnvelopeSize != null) {
        	maxEnvelopeSizeElement = FACTORY.createMaxEnvelopeSize(maxEnvelopeSize);
        }

        JAXBElement<AttributablePositiveInteger> maxElementsElement = null;
        if (maxElements != null) {
            final AttributablePositiveInteger maxInteger = FACTORY.createAttributablePositiveInteger();
            maxInteger.setValue(new BigInteger(Long.toString(maxElements)));
            maxElementsElement = FACTORY.createMaxElements(maxInteger);
        }

        JAXBElement<AttributableDuration> maxTimeElement = null;
        if (maxTime != null) {
            final AttributableDuration maxDuration = FACTORY.createAttributableDuration();
            maxDuration.setValue(maxTime);
            maxTimeElement = FACTORY.createMaxTime(maxDuration);
        }

        super.setSubscribe(endTo, deliveryMode, notifyTo, expires, filter,
                retryElement, heartbeatsElement, sendBookmarksElement, bookmarkElement,
                maxEnvelopeSizeElement, maxElementsElement, maxTimeElement);
    }

    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires,
            final Object context)
            throws SOAPException, JAXBException {

        final EnumerationContextType contextType = Enumeration.FACTORY.createEnumerationContextType();
        contextType.getContent().add(context);
        // TODO: this should have been generated by JAXB as - createEnumerationContextType(contextType);
        final JAXBElement<EnumerationContextType> contextTypeElement =
                new JAXBElement<EnumerationContextType>(Enumeration.ENUMERATION_CONTEXT, EnumerationContextType.class, null, contextType);
        super.setSubscribeResponse(mgr, expires, contextTypeElement);
    }


	public DialectableMixedDataType getWsmanFilter() throws JAXBException, SOAPException {
		Subscribe subscribe = getSubscribe();

		if (subscribe == null) {
			return null;
		}
		return (DialectableMixedDataType) extract(subscribe.getAny(),
				                                  DialectableMixedDataType.class,
				                                  FILTER);
	}

    private static Object extract(final List<Object> anyList, final Class classType, final QName eltName) {
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
}
