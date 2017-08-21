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
 **$Log: Eventing.java,v $
 **Revision 1.13  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 **
 * $Id: Eventing.java,v 1.13 2007/05/30 20:31:05 nbeers Exp $
 */

package com.sun.ws.management.eventing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.eventing.DeliveryType;
import org.xmlsoap.schemas.ws._2004._08.eventing.FilterType;
import org.xmlsoap.schemas.ws._2004._08.eventing.GetStatus;
import org.xmlsoap.schemas.ws._2004._08.eventing.GetStatusResponse;
import org.xmlsoap.schemas.ws._2004._08.eventing.LanguageSpecificStringType;
import org.xmlsoap.schemas.ws._2004._08.eventing.ObjectFactory;
import org.xmlsoap.schemas.ws._2004._08.eventing.Renew;
import org.xmlsoap.schemas.ws._2004._08.eventing.RenewResponse;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._08.eventing.SubscribeResponse;
import org.xmlsoap.schemas.ws._2004._08.eventing.SubscriptionEnd;
import org.xmlsoap.schemas.ws._2004._08.eventing.Unsubscribe;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.xml.XML;

public class Eventing extends Addressing {

    public static final String NS_PREFIX = "wse";
    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing";

    public static final String SUBSCRIBE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/Subscribe";
    public static final String SUBSCRIBE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/SubscribeResponse";
    public static final String RENEW_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/Renew";
    public static final String RENEW_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/RenewResponse";
    public static final String GET_STATUS_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/GetStatus";
    public static final String GET_STATUS_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/GetStatusResponse";
    public static final String UNSUBSCRIBE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/Unsubscribe";
    public static final String UNSUBSCRIBE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/UnsubscribeResponse";
    public static final String SUBSCRIPTION_END_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/SubscriptionEnd";

    public static final String PUSH_DELIVERY_MODE = "http://schemas.xmlsoap.org/ws/2004/08/eventing/DeliveryModes/Push";

    public static final String DELIVERY_FAILURE_STATUS = "http://schemas.xmlsoap.org/ws/2004/08/eventing/DeliveryFailure";
    public static final String SOURCE_SHUTTING_DOWN_STATUS = "http://schemas.xmlsoap.org/ws/2004/08/eventing/SourceShuttingDown";
    public static final String SOURCE_CANCELING_STATUS = "http://schemas.xmlsoap.org/ws/2004/08/eventing/SourceCanceling";

    public static final String FAULT_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/08/eventing/fault";

    public static final QName INVALID_MESSAGE = new QName(NS_URI, "InvalidMessage", NS_PREFIX);
    public static final String INVALID_MESSAGE_REASON =
            "The request message had unknown or invalid content and could not be processed.";

    public static final QName SUBSCRIBE = new QName(NS_URI, "Subscribe", NS_PREFIX);
    public static final QName SUBSCRIBE_RESPONSE = new QName(NS_URI, "SubscribeResponse", NS_PREFIX);
    public static final QName RENEW = new QName(NS_URI, "Renew", NS_PREFIX);
    public static final QName RENEW_RESPONSE = new QName(NS_URI, "RenewResponse", NS_PREFIX);
    public static final QName GET_STATUS = new QName(NS_URI, "GetStatus", NS_PREFIX);
    public static final QName GET_STATUS_RESPONSE = new QName(NS_URI, "GetStatusResponse", NS_PREFIX);
    public static final QName UNSUBSCRIBE = new QName(NS_URI, "Unsubscribe", NS_PREFIX);
    public static final QName SUBSCRIPTION_END = new QName(NS_URI, "SubscriptionEnd", NS_PREFIX);
    public static final QName IDENTIFIER = new QName(NS_URI, "Identifier", NS_PREFIX);
    public static final QName NOTIFY_TO = new QName(NS_URI, "NotifyTo", NS_PREFIX);
    public static final QName FILTER = new QName(NS_URI, "Filter", NS_PREFIX);

    public static final ObjectFactory FACTORY = new ObjectFactory();

    public Eventing() throws SOAPException {
        super();
    }

    public Eventing(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public Eventing(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public void setSubscribe(final EndpointReferenceType endTo, final String deliveryMode,
            final EndpointReferenceType notifyTo, final String expires, final FilterType filter,
            final Object... extensions)
            throws SOAPException, JAXBException {

        removeChildren(getBody(), SUBSCRIBE);
        final Subscribe sub = FACTORY.createSubscribe();

        if (endTo != null) {
            sub.setEndTo(endTo);
        }

        final DeliveryType delivery = FACTORY.createDeliveryType();

        if (deliveryMode != null) {
            delivery.setMode(deliveryMode);
        }

        if (notifyTo != null) {
        	delivery.getContent().add((FACTORY.createNotifyTo(notifyTo)));
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

        getXmlBinding().marshal(sub, getBody());
    }

    public void setSubscribeResponse(final EndpointReferenceType mgr, final String expires,
            final Object... extensions)
            throws SOAPException, JAXBException {

        removeChildren(getBody(), SUBSCRIBE_RESPONSE);
        final SubscribeResponse response = FACTORY.createSubscribeResponse();
        response.setSubscriptionManager(mgr);
        response.setExpires(expires);
        if (extensions != null) {
            for (final Object ext : extensions) {
                response.getAny().add(ext);
            }
        }
        getXmlBinding().marshal(response, getBody());
    }

    public void setRenew(final String expires) throws SOAPException, JAXBException {
        removeChildren(getBody(), RENEW);
        final Renew renew = FACTORY.createRenew();
        renew.setExpires(expires.trim());
        getXmlBinding().marshal(renew, getBody());
    }

    public void setRenewResponse(final String expires) throws SOAPException, JAXBException {
        removeChildren(getBody(), RENEW_RESPONSE);
        final RenewResponse response = FACTORY.createRenewResponse();
        response.setExpires(expires);
        getXmlBinding().marshal(response, getBody());
    }

    public void setGetStatus() throws SOAPException, JAXBException {
        removeChildren(getBody(), GET_STATUS);
        final GetStatus status = FACTORY.createGetStatus();
        getXmlBinding().marshal(status, getBody());
    }

    public void setGetStatusResponse(final String expires) throws SOAPException, JAXBException {
        removeChildren(getBody(), GET_STATUS_RESPONSE);
        final GetStatusResponse response = FACTORY.createGetStatusResponse();
        response.setExpires(expires);
        getXmlBinding().marshal(response, getBody());
    }

    public void setUnsubscribe() throws SOAPException, JAXBException {
        removeChildren(getBody(), UNSUBSCRIBE);
        final Unsubscribe unsub = FACTORY.createUnsubscribe();
        getXmlBinding().marshal(unsub, getBody());
    }

    public void setSubscriptionEnd(final EndpointReferenceType mgr,
            final String status, final String reason) throws SOAPException, JAXBException {

        if (!DELIVERY_FAILURE_STATUS.equals(status) &&
                !SOURCE_SHUTTING_DOWN_STATUS.equals(status) &&
                !SOURCE_CANCELING_STATUS.equals(status)) {
            throw new IllegalArgumentException("Status must be one of " +
                    DELIVERY_FAILURE_STATUS + ", " +
                    SOURCE_SHUTTING_DOWN_STATUS + " or " +
                    SOURCE_CANCELING_STATUS);
        }

        removeChildren(getBody(), SUBSCRIPTION_END);
        final SubscriptionEnd end = FACTORY.createSubscriptionEnd();
        end.setSubscriptionManager(mgr);
        end.setStatus(status);

        if (reason != null) {
            final LanguageSpecificStringType localizedReason = FACTORY.createLanguageSpecificStringType();
            localizedReason.setLang(XML.DEFAULT_LANG);
            localizedReason.setValue(reason);
            end.getReason().add(localizedReason);
        }

        getXmlBinding().marshal(end, getBody());
    }

    public void setIdentifier(final String identifier) throws SOAPException, JAXBException {
        removeChildren(getHeader(), IDENTIFIER);
        getXmlBinding().marshal(FACTORY.createIdentifier(identifier), getHeader());
    }

    public Subscribe getSubscribe() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), SUBSCRIBE);
        return (Subscribe) value;
    }

    public SubscribeResponse getSubscribeResponse() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), SUBSCRIBE_RESPONSE);
        return (SubscribeResponse) value;
    }

    public Renew getRenew() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), RENEW);
        return (Renew) value;
    }

    public RenewResponse getRenewResponse() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), RENEW_RESPONSE);
        return (RenewResponse) value;
    }

    public GetStatus getGetStatus() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), GET_STATUS);
        return (GetStatus) value;
    }

    public GetStatusResponse getGetStatusResponse() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), GET_STATUS_RESPONSE);
        return (GetStatusResponse) value;
    }

    public Unsubscribe getUnsubscribe() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), UNSUBSCRIBE);
        return (Unsubscribe) value;
    }

    public SubscriptionEnd getSubscriptionEnd() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), SUBSCRIPTION_END);
        return (SubscriptionEnd) value;
    }

    public String getIdentifier() throws JAXBException, SOAPException {
        final JAXBElement<String> value =
        	  (JAXBElement<String>) unbind(getHeader(), IDENTIFIER);
        return value.getValue();
    }

    // retrieve the current subscription EPR
    public EndpointReferenceType getSubscriptionManagerEpr() throws JAXBException, SOAPException {

        SubscribeResponse response = this.getSubscribeResponse();
		if (response == null) {
			return null;
		} else {
			return response.getSubscriptionManager();
		}
    }

    public void setSubscriptionManagerEpr(EndpointReferenceType mgr) throws JAXBException, SOAPException {

    	SubscribeResponse response = this.getSubscribeResponse();
    	if (response == null) {
    		this.setSubscribeResponse(mgr, null);
    	} else {
    		removeChildren(getBody(), SUBSCRIBE_RESPONSE);
    		response.setSubscriptionManager(mgr);
    		getXmlBinding().marshal(response, getBody());
    	}
    }

    public void addRefParamsToSubscriptionManagerEpr(List<Object> list) throws JAXBException, SOAPException {

    	EndpointReferenceType mgr = getSubscriptionManagerEpr();

    	if (mgr == null) {
    		throw new IllegalStateException("Subscription Manager EPR is not set.");
    	} else {
			ReferenceParametersType refs = mgr.getReferenceParameters();
			if (refs == null) {
				refs = new ReferenceParametersType();
				mgr.setReferenceParameters(refs);
			}
			List<Object> params = refs.getAny();
			for (final Object obj : list) {
				params.add(obj);
			}
			setSubscriptionManagerEpr(mgr);
    	}
    }

}
