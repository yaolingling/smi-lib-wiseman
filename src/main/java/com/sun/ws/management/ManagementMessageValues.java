/*
 * Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt 
 **
 **$Log: ManagementMessageValues.java,v $
 **Revision 1.5  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: ManagementMessageValues.java,v 1.5 2007/05/30 20:31:05 nbeers Exp $
 */
package com.sun.ws.management;

import java.math.BigInteger;
import java.util.Set;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.Locale;
import org.dmtf.schemas.wbem.wsman._1.wsman.OptionType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;

import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.xml.XmlBinding;

/**This class is meant to be a container for constants
 * that are used or applied to an Management message.
 * No complicated methods or logic should be present.
 * Many of the fields in this class are defined as final
 * static and public so that other message classes may 
 * refer to ManagementMessageValues as the root property
 * definition class.
 * 
 * @author Simeon Pinder
 */
public class ManagementMessageValues {
	
	//static defaults
	public static final long DEFAULT_TIMEOUT =30000;
	public static final String DEFAULT_UID_SCHEME ="uuid:";
	//These must be overridden for any messages.
	public static final String DEFAULT_RESOURCE_URI = "";
	public static final String DEFAULT_TO = "";
	public static final String DEFAULT_REPLY_TO = Addressing.ANONYMOUS_ENDPOINT_URI;
	public static final Vector<ReferenceParametersType> DEFAULT_ADDITIONAL_HEADERS = 
		new Vector<ReferenceParametersType>();
	public static final Set<SelectorType> DEFAULT_SELECTOR_SET = null;
	public static final BigInteger DEFAULT_MAX_ENVELOPE_SIZE = BigInteger.valueOf(0);
	public static final Locale DEFAULT_LOCALE = null;
	public static final Set<OptionType> DEFAULT_OPTION_SET = null;
	public static final String WSMAN_DESTINATION="http://localhost:8080/wsman/";
	
	/*These values are added so that each instance has
	 * properties that can be get/set for all the relevant
	 * fields.
	 */
	//Instance level values
	private long timeout = DEFAULT_TIMEOUT;
	private String uidScheme = DEFAULT_UID_SCHEME;
	private String resourceUri = DEFAULT_RESOURCE_URI;
	private String to = DEFAULT_TO;
	private String replyTo = DEFAULT_REPLY_TO;
	private XmlBinding xmlBinding = null;
	private Vector<ReferenceParametersType> additionalHeaders = 
		DEFAULT_ADDITIONAL_HEADERS;
	private Set<SelectorType> selectorSet = DEFAULT_SELECTOR_SET;
	private BigInteger maxEnvelopeSize = DEFAULT_MAX_ENVELOPE_SIZE;
	private Locale locale = DEFAULT_LOCALE;
	private Set<OptionType> optionSet = DEFAULT_OPTION_SET;
	
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public BigInteger getMaxEnvelopeSize() {
		return maxEnvelopeSize;
	}

	public void setMaxEnvelopeSize(BigInteger maxEnvelopeSize) {
		this.maxEnvelopeSize = maxEnvelopeSize;
	}

	/** Instance has all the default values set. An empty
	 * instance of Management is lazily instantiated to 
	 * obtain a valid xmlbinding instance.
	 * 
	 * @throws SOAPException
	 */
	public ManagementMessageValues() throws SOAPException{
		if(xmlBinding==null){
		  xmlBinding = new Management().getXmlBinding();
		}
	}
	
	public static ManagementMessageValues newInstance() throws SOAPException{
		return new ManagementMessageValues();
	}
	
	//############ GETTERS/SETTERS for the instance variables.
	/**
	 * @return the resourceUri
	 */
	public String getResourceUri() {
		return resourceUri;
	}
	/**
	 * @param resourceUri the resourceUri to set
	 */
	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}
	/**
	 * @return the timeout
	 */
	public long getTimeout() {
		return timeout;
	}
	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	/**
	 * @return the to
	 */
	public String getTo() {
		return to;
	}
	/**
	 * @param to the to to set
	 */
	public void setTo(String to) {
		this.to = to;
	}
	/**
	 * @return the uidScheme
	 */
	public String getUidScheme() {
		return uidScheme;
	}
	/**
	 * @param uidScheme the uidScheme to set
	 */
	public void setUidScheme(String uidScheme) {
		this.uidScheme = uidScheme;
	}

	/**
	 * @return the xmlBinding
	 */
	public XmlBinding getXmlBinding() {
		return xmlBinding;
	}

	/**
	 * @param binding the xmlBinding to set
	 */
	public void setXmlBinding(XmlBinding binding) {
		this.xmlBinding = binding;
	}

	public boolean addCustomHeader(QName customHeader, String nodeValue) {
		boolean success = false;
		if((customHeader==null)){
			return success;
		}
		ReferenceParametersType customNode = 
			Addressing.createReferenceParametersType(
					customHeader,
					nodeValue);
		if(customNode!=null){
			additionalHeaders.add(customNode);
			success=true;
		}
		return success;
	}

	/**
	 * @return the additionalHeaders
	 */
	public Vector<ReferenceParametersType> getAdditionalHeaders() {
		return additionalHeaders;
	}

	/**
	 * @param additionalHeaders the additionalHeaders to set
	 */
	public void setAdditionalHeaders(
			Vector<ReferenceParametersType> additionalHeaders) {
		this.additionalHeaders = additionalHeaders;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public Set<SelectorType> getSelectorSet() {
		return selectorSet;
	}

	public void setSelectorSet(Set<SelectorType> selectorSet) {
		this.selectorSet = selectorSet;
	}

	public Set<OptionType> getOptionSet() {
		return optionSet;
	}

	public void setOptionSet(Set<OptionType> optionSet) {
		this.optionSet = optionSet;
	}

	
}
