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
 **Revision 1.14  2007/05/30 20:31:03  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EnumerationExtensions.java,v 1.15 2007-10-30 09:27:30 jfdenise Exp $
 */

package com.sun.ws.management.enumeration;

import com.sun.ws.management.MessageUtil;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.AnyListType;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableEmpty;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableNonNegativeInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.AttributablePositiveInteger;
import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;
import org.dmtf.schemas.wbem.wsman._1.wsman.ItemType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.xml.XmlBinding;

public class EnumerationExtensions extends Enumeration {

    public static final QName REQUEST_TOTAL_ITEMS_COUNT_ESTIMATE =
            new QName(Management.NS_URI, "RequestTotalItemsCountEstimate", Management.NS_PREFIX);

    public static final QName TOTAL_ITEMS_COUNT_ESTIMATE =
            new QName(Management.NS_URI, "TotalItemsCountEstimate", Management.NS_PREFIX);

    public static final QName OPTIMIZE_ENUMERATION =
            new QName(Management.NS_URI, "OptimizeEnumeration", Management.NS_PREFIX);

    public static final QName MAX_ELEMENTS =
            new QName(Management.NS_URI, "MaxElements", Management.NS_PREFIX);

    public static final QName ENUMERATION_MODE =
            new QName(Management.NS_URI, "EnumerationMode", Management.NS_PREFIX);

    public static final QName ITEMS =
            new QName(Management.NS_URI, "Items", Management.NS_PREFIX);

    public static final QName ITEM =
            new QName(Management.NS_URI, "Item", Management.NS_PREFIX);

    public static final QName END_OF_SEQUENCE =
            new QName(Management.NS_URI, "EndOfSequence", Management.NS_PREFIX);

    public static final QName FILTER =
        new QName(Management.NS_URI, "Filter", Management.NS_PREFIX);

    final static String WSMAN_ITEM = ITEM.getPrefix()+":"+ITEM.getLocalPart();

    public enum Mode {
        EnumerateEPR("EnumerateEPR"),
        EnumerateObjectAndEPR("EnumerateObjectAndEPR");

        public static Mode fromBinding(final JAXBElement<EnumerationModeType> t) {
            return valueOf(t.getValue().value());
        }

        private String mode;
        Mode(final String m) { mode = m; }
        public JAXBElement<EnumerationModeType> toBinding() {
            return Management.FACTORY.createEnumerationMode(EnumerationModeType.fromValue(mode));
        }
    }

    public EnumerationExtensions() throws SOAPException {
        super();
    }

    public EnumerationExtensions(final Addressing addr) throws SOAPException {
        super(addr);
    }

    public EnumerationExtensions(final InputStream is) throws SOAPException, IOException {
        super(is);
    }

    public void setEnumerate(final EndpointReferenceType endTo,
    		                 final boolean requestTotalItemsCountEstimate,
                             final boolean optimize,
                             final int maxElements,
                             final String expires,
                             final DialectableMixedDataType filter,
                             final Mode mode,
                             final Object... anys) throws JAXBException, SOAPException {

        final List<Object> list =  MessageUtil.createEnumerateList(optimize,maxElements,
               filter, mode, anys);
        super.setEnumerate(endTo, expires, null, list.toArray());
        if (requestTotalItemsCountEstimate)
           setRequestTotalItemsCountEstimate();
    }

    // context must not be null, the others can be null
    // context must be either java.lang.String or org.w3c.dom.Element
    public void setPull(final Object context, final int maxChars,
            final int maxElements, final Duration maxDuration,
            final boolean requestTotalItemsCountEstimate)
            throws JAXBException, SOAPException, DatatypeConfigurationException {

    	super.setPull(context, maxChars, maxElements, maxDuration);
        if (requestTotalItemsCountEstimate)
            setRequestTotalItemsCountEstimate();
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
				super.setEnumerateResponse(context, expires, anyList, eos);
			} else {
				super.setEnumerateResponse(context, expires, anyList);
			}
		} else {

			super.setEnumerateResponse(context, expires);
		}
    }

    public void setPullResponse(final List<EnumerationItem> items, final Object context, final boolean haveMore, EnumerationModeType mode)
    throws JAXBException, SOAPException {

        final ItemListType itemList = FACTORY.createItemListType();
        final List<Object> itemListAny = itemList.getAny();
        // go through each element in the list and add appropriate item to list
        // depending on the EnumerationModeType
        for (final EnumerationItem ee : items) {
        	MessageUtil.addEnumerationItem(itemListAny,ee,mode);
        }

        super.setPullResponse(itemList, context, haveMore);
    }

    public List<EnumerationItem> getItems() throws JAXBException, SOAPException {
        
        final PullResponse pullResponse = getPullResponse();
        final EnumerateResponse enumerateResponse = getEnumerateResponse();
        return MessageUtil.getItems(pullResponse, enumerateResponse);
        
    }

    public boolean isEndOfSequence()
    throws JAXBException, SOAPException {
    	final PullResponse pullResponse = getPullResponse();
        final EnumerateResponse enumerateResponse = getEnumerateResponse();
    	return MessageUtil.isEndOfSequence(pullResponse, enumerateResponse);
    }

    private int getNextElementIndex(NodeList list, int start) {
        for(int i = start; i < list.getLength(); i++) {
            Node n = list.item(i);
            int type = n.getNodeType();
            if(type == Node.ELEMENT_NODE) {
                return i;
            }
        }
        return -1;
    }

   
    public void setRequestTotalItemsCountEstimate() throws JAXBException {
        final AttributableEmpty empty = new AttributableEmpty();
        final JAXBElement<AttributableEmpty> emptyElement =
                Management.FACTORY.createRequestTotalItemsCountEstimate(empty);
        getXmlBinding().marshal(emptyElement, getHeader());
    }

    public AttributableEmpty getRequestTotalItemsCountEstimate() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), REQUEST_TOTAL_ITEMS_COUNT_ESTIMATE);
        return value == null ? null : ((JAXBElement<AttributableEmpty>) value).getValue();
    }

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
        getXmlBinding().marshal(countElement, getHeader());
    }

    public AttributableNonNegativeInteger getTotalItemsCountEstimate() throws JAXBException, SOAPException {
        final Object value = unbind(getHeader(), TOTAL_ITEMS_COUNT_ESTIMATE);
        return value == null ? null : ((JAXBElement<AttributableNonNegativeInteger>) value).getValue();
    }

	public DialectableMixedDataType getWsmanFilter() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return null;
		}
		return (DialectableMixedDataType) MessageUtil.extract(enumerate.getAny(),
				                                  DialectableMixedDataType.class,
				                                  FILTER);
	}

	public EnumerationModeType getModeType() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return null;
		}
		EnumerationModeType type =  (EnumerationModeType) MessageUtil.extract(enumerate.getAny(),
				                                                  EnumerationModeType.class,
		                                                          ENUMERATION_MODE);
		return (type == null) ? null : type;
	}

	public Mode getMode() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return null;
		}
		EnumerationModeType type =  getModeType();
		return (type == null) ? null : Mode.valueOf(type.value());
	}

	public boolean getOptimize() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return false;
		}
		AttributableEmpty optimize =  (AttributableEmpty) MessageUtil.extract(enumerate.getAny(),
                                               AttributableEmpty.class,
				                               OPTIMIZE_ENUMERATION);
		return (optimize == null) ? false : true;
	}

	public int getMaxElements() throws JAXBException, SOAPException {
		Enumerate enumerate = getEnumerate();

		if (enumerate == null) {
			return 1;
		}
		AttributablePositiveInteger max =  (AttributablePositiveInteger) MessageUtil.extract(enumerate.getAny(),
				AttributablePositiveInteger.class,
				MAX_ELEMENTS);

		return (max == null) ? 1 : max.getValue().intValue();
	}

	public void setFilterNamespaces(Map<String, String> namespaces)
			throws SOAPException {
		SOAPBody body = getBody();

		// Add the namespaces to the body.
		if (namespaces != null) {
			Set<String> prefixes = namespaces.keySet();
			Iterator prefixIterator = prefixes.iterator();
			while (prefixIterator.hasNext()) {
				String prefix = (String) prefixIterator.next();
				String uri = namespaces.get(prefix);
				body.addNamespaceDeclaration(prefix, uri);
			}
		}
	}
}
