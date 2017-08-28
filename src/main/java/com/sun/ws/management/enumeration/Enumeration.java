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
 **$Log: Enumeration.java,v $
 **Revision 1.20  2007/06/18 17:57:11  nbeers
 **Fix for Issue #119 (EnumerationUtility.buildMessage() generates incorrect msg).
 **
 **Revision 1.19  2007/05/30 20:31:03  nbeers
 **Add HP copyright header
 **
 **
 * $Id: Enumeration.java,v 1.20 2007/06/18 17:57:11 nbeers Exp $
 */

package com.sun.ws.management.enumeration;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationEnd;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ObjectFactory;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Release;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.server.EnumerationItem;


public class Enumeration extends Addressing {

    public static final String NS_PREFIX = "wsen";
    public static final String NS_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";

    public static final String ENUMERATE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate";
    public static final String ENUMERATE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse";

    public static final String PULL_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull";
    public static final String PULL_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse";

    public static final String RENEW_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Renew";
    public static final String RENEW_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/RenewResponse";

    public static final String GET_STATUS_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetStatus";
    public static final String GET_STATUS_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/GetStatusResponse";

    public static final String RELEASE_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Release";
    public static final String RELEASE_RESPONSE_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/ReleaseResponse";

    public static final String ENUMERATION_END_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerationEnd";

    public static final String SOURCE_SHUTTING_DOWN_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/SourceShuttingDown";
    public static final String SOURCE_CANCELING_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/SourceCanceling";

    public static final String FAULT_ACTION_URI = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/fault";

    public static final QName ENUMERATE = new QName(NS_URI, "Enumerate", NS_PREFIX);
    public static final QName ENUMERATE_RESPONSE = new QName(NS_URI, "EnumerateResponse", NS_PREFIX);
    public static final QName PULL = new QName(NS_URI, "Pull", NS_PREFIX);
    public static final QName PULL_RESPONSE = new QName(NS_URI, "PullResponse", NS_PREFIX);
    public static final QName RELEASE = new QName(NS_URI, "Release", NS_PREFIX);
    public static final QName ENUMERATION_END = new QName(NS_URI, "EnumerationEnd", NS_PREFIX);
    public static final QName SUPPORTED_DIALECT = new QName(NS_URI, "SupportedDialect", NS_PREFIX);
    public static final QName ENUMERATION_CONTEXT = new QName(NS_URI, "EnumerationContext", NS_PREFIX);
    public static final QName FILTER = new QName(NS_URI, "Filter", NS_PREFIX);

    public static final ObjectFactory FACTORY = new ObjectFactory();

    public Enumeration() throws SOAPException {
        super();
    }

    public Enumeration(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public Enumeration(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public void setEnumerate(final EndpointReferenceType endTo,
            final String expires, final FilterType filter,
            final Object... anys)
            throws JAXBException, SOAPException {

        removeChildren(getBody(), ENUMERATE);
        final Enumerate enu = FACTORY.createEnumerate();
        if (endTo != null) {
            enu.setEndTo(endTo);
        }
        if (expires != null) {
            enu.setExpires(expires.trim());
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
        getXmlBinding().marshal(enu, getBody());
    }


    public void setEnumerateResponse(final Object context, final String expires, final Object... anys)
    throws JAXBException, SOAPException {

        removeChildren(getBody(), ENUMERATE_RESPONSE);
        final EnumerateResponse response = FACTORY.createEnumerateResponse();

        final EnumerationContextType contextType = FACTORY.createEnumerationContextType();
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
        getXmlBinding().marshal(response, getBody());
    }

    // context must not be null, the others can be null
    // context must be either java.lang.String or org.w3c.dom.Element
    public void setPull(final Object context, final int maxChars,
            final int maxElements, final Duration maxDuration)
            throws JAXBException, SOAPException, DatatypeConfigurationException {

        removeChildren(getBody(), PULL);
        removeChildren(getBody(), ENUMERATE);
        final Pull pull = FACTORY.createPull();

        final EnumerationContextType contextType = FACTORY.createEnumerationContextType();
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

        getXmlBinding().marshal(pull, getBody());
    }

    public void setPullResponse(final List<EnumerationItem> items, final Object context, final boolean haveMore)
    throws JAXBException, SOAPException {

        final ItemListType itemList = FACTORY.createItemListType();
		final List<Object> itemListAny = itemList.getAny();
		// go through each element in the list and add appropriate item to list
		// depending on the EnumerationModeType
		for (final EnumerationItem ee : items) {
			itemListAny.add(ee.getItem());
		}
		setPullResponse(itemList, context, haveMore);
    }

    protected void setPullResponse(final ItemListType itemList, final Object context, final boolean haveMore)
    throws JAXBException, SOAPException {

        removeChildren(getBody(), PULL_RESPONSE);
        final PullResponse response = FACTORY.createPullResponse();

		response.setItems(itemList);

        if (haveMore) {
            final EnumerationContextType contextType = FACTORY.createEnumerationContextType();
            contextType.getContent().add(context);
            response.setEnumerationContext(contextType);
        } else {
            response.setEndOfSequence("");
        }

        getXmlBinding().marshal(response, getBody());
    }

    public void setRelease(final Object context) throws JAXBException, SOAPException {
        removeChildren(getBody(), RELEASE);
        final Release release = FACTORY.createRelease();

        final EnumerationContextType contextType = FACTORY.createEnumerationContextType();
        contextType.getContent().add(context);
        release.setEnumerationContext(contextType);

        getXmlBinding().marshal(release, getBody());
    }

    public Enumerate getEnumerate() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), ENUMERATE);
        return value == null ? null : (Enumerate) value;
    }

    public EnumerateResponse getEnumerateResponse() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), ENUMERATE_RESPONSE);
        return value == null ? null : (EnumerateResponse) value;
    }

    public Pull getPull() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), PULL);
        return value == null ? null : (Pull) value;
    }

    public PullResponse getPullResponse() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), PULL_RESPONSE);
        return value == null ? null : (PullResponse) value;
    }

    public Release getRelease() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), RELEASE);
        return value == null ? null : (Release) value;
    }

    public EnumerationEnd getEnumerationEnd() throws JAXBException, SOAPException {
        final Object value = unbind(getBody(), ENUMERATION_END);
        return value == null ? null : (EnumerationEnd) value;
    }

    public boolean isEndOfSequence()
    throws JAXBException, SOAPException {
    	Object eos = null;
    	final PullResponse pullResponse = getPullResponse();
    	if (pullResponse != null) {
    		eos = pullResponse.getEndOfSequence();
    	}
        return null != eos;
    }

	public FilterType getFilter() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return null;
		}
		return enumerate.getFilter();
	}
}
