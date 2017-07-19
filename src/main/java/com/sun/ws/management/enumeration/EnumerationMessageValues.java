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
 **$Log: not supported by cvs2svn $
 **Revision 1.4  2007/05/30 20:31:03  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: EnumerationMessageValues.java,v 1.5 2007-06-18 17:57:11 nbeers Exp $
 */
package com.sun.ws.management.enumeration;

import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.DialectableMixedDataType;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementMessageValues;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.EnumerationExtensions.Mode;

/**This class is meant to be a container for constants
 * that are used or applied to an Enumeration message.
 * No complicated methods or logic should be present.
 * 
 * See following for original settings as ManagemetMessageConstant
 * values apply.
 * {@link ManagementMessageValues}
 *  
 * @author Simeon Pinder
 */
public class EnumerationMessageValues extends ManagementMessageValues{
	
	public EnumerationMessageValues() throws SOAPException {
		super();
		// TODO Auto-generated constructor stub
	}

	//In event of failure and to initialize these values are defined
	public static final String DEFAULT_UID_SCHEME=ManagementMessageValues.DEFAULT_UID_SCHEME;
	public static long DEFAULT_MAXTIME_VALUE =ManagementMessageValues.DEFAULT_TIMEOUT;
	public static final String DEFAULT_ENUMERATION_MESSAGE_ACTION_TYPE =
		Enumeration.ENUMERATE_ACTION_URI;
	public static final String DEFAULT_FILTER = "";
	public static final String DEFAULT_FILTER_DIALECT = com.sun.ws.management.xml.XPath.NS_URI;
	public static final String DEFAULT_REPLY_TO = Addressing.ANONYMOUS_ENDPOINT_URI;
	public static final int DEFAULT_MAX_CHARACTERS = 0;
	public static final int DEFAULT_MAX_ELEMENTS = 10 ;
	public static final String DEFAULT_ENUMERATION_CONTEXT = "";
	public static final String[] DEFAULT_CUSTOM_XML_BINDINGS = {};
	public static final Mode DEFAULT_ENUMERATION_MODE = null;
	public static final boolean DEFAULT_REQUEST_FOR_ITEMS_TOTAL = false;
	public static final boolean DEFAULT_REQUEST_TO_OPTIMIZE_ENUMERATION = false;
	public static final Map<String, String> DEFAULT_NS_MAP = null;
	private static DatatypeFactory factory = null;
	{//static initialization block
	  try{
		 factory = DatatypeFactory.newInstance();
	  }catch(Exception ex){
		 //eat the exception and move on. This really shouldn't fail. Is fundamental failure   
	  }
	}
	public static final String DEFAULT_END_TO = "";
	public static long DEFAULT_EXPIRES =ManagementMessageValues.DEFAULT_TIMEOUT;
	
	
	//Default property values.
	private String uidScheme = DEFAULT_UID_SCHEME;
	private long maxTime =DEFAULT_MAXTIME_VALUE;
	private String enumerationMessageActionType =
		DEFAULT_ENUMERATION_MESSAGE_ACTION_TYPE;
	private String filter = DEFAULT_FILTER;
	private String filterDialect = DEFAULT_FILTER_DIALECT;
	private String replyTo = DEFAULT_REPLY_TO;
	private Object enumerationContext = DEFAULT_ENUMERATION_CONTEXT;
	private int maxElements = DEFAULT_MAX_ELEMENTS;
	private int maxCharacters = DEFAULT_MAX_CHARACTERS;
	private String[] customXmlBindingPackageList =DEFAULT_CUSTOM_XML_BINDINGS;
	private Mode enumerationMode = DEFAULT_ENUMERATION_MODE;
	private boolean requestForTotalItemsCount = DEFAULT_REQUEST_FOR_ITEMS_TOTAL;
	private boolean requestForOptimizedEnumeration = 
		DEFAULT_REQUEST_TO_OPTIMIZE_ENUMERATION;
	private Map<String, String> namespaceMap = DEFAULT_NS_MAP;
	private String endTo = DEFAULT_END_TO;
	private long expires = DEFAULT_EXPIRES;
	
	/**The following static factory method should be used to 
	 * retrieve a default instance of teh EnumerationMessageValues
	 * class with all of the associated defaults applied.
	 * 
	 * NOTE: You should explicitly set the Action for the message as 
	 * it defaults to Enumeration.ENUMERATION_ACTION_URI.
	 * 
	 * @return EnumerationMessageConstants instance with defaults set.
	 * @throws SOAPException 
	 */
	public static EnumerationMessageValues newInstance() throws SOAPException {
		return new EnumerationMessageValues();
	}

	public static Duration newDuration(long duration) throws DatatypeConfigurationException{
		Duration value = null;
		if(factory==null){
		  factory = DatatypeFactory.newInstance();
		}
		value = factory.newDuration(duration);
		return value;
	}
	
	public static DialectableMixedDataType newFilter(String expression,
			String dialect) {
      final DialectableMixedDataType filter;
      if ((expression != null) && (expression.length() > 0)) {
          filter = Management.FACTORY.createDialectableMixedDataType();
          if ((dialect != null) && (dialect.length() > 0))
              filter.setDialect(dialect);
          filter.getContent().add(expression);
      } else {
      	filter = null;
      }
      return filter;
	}
	
	//############# GETTERS/SETTER for the default field values
	/**
	 * @return the defaultTimeout
	 */
	public long getMaxTime() {
		return maxTime;
	}

	/**
	 * @param defaultTimeout the defaultTimeout to set
	 */
	public void setMaxTime(long defaultTimeout) {
		this.maxTime = defaultTimeout;
	}

	/**
	 * @return the enumerationMessageActionType
	 */
	public String getEnumerationMessageActionType() {
		return enumerationMessageActionType;
	}

	/**
	 * @param enumerationMessageType the enumerationMessageType to set
	 */
	public void setEnumerationMessageActionType(String enumerationMessageType) {
		this.enumerationMessageActionType = enumerationMessageType;
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
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}


	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}


	/**
	 * @return the filterDialect
	 */
	public String getFilterDialect() {
		return filterDialect;
	}


	/**
	 * @param filterDialect the filterDialect to set
	 */
	public void setFilterDialect(String filterDialect) {
		this.filterDialect = filterDialect;
	}


	/**
	 * @return the replyTo
	 */
	public String getReplyTo() {
		return replyTo;
	}


	/**
	 * @param replyTo the replyTo to set
	 */
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}


	/**
	 * @return the enumerationContext
	 */
	public Object getEnumerationContext() {
		return enumerationContext;
	}


	/**
	 * @param context the enumerationContext to set
	 */
	public void setEnumerationContext(Object context) {
		this.enumerationContext = context;
	}


	/**
	 * @return the maxCharacters
	 */
	public int getMaxCharacters() {
		return maxCharacters;
	}


	/**
	 * @param maxCharacters the maxCharacters to set
	 */
	public void setMaxCharacters(int maxCharacters) {
		this.maxCharacters = maxCharacters;
	}


	/**
	 * @return the maxElements
	 */
	public int getMaxElements() {
		return maxElements;
	}


	/**
	 * @param maxElements the maxElements to set
	 */
	public void setMaxElements(int maxElements) {
		this.maxElements = maxElements;
	}

	/**
	 * @return the customXmlBindingPackageList
	 */
	public String[] getCustomXmlBindingPackageList() {
		return customXmlBindingPackageList;
	}

	/**
	 * @param customPackageList the customPackageList to set
	 */
	public void setCustomPackageList(String[] customPackageList) {
		this.customXmlBindingPackageList = customPackageList;
	}

	public void setEnumerationMode(Mode mode) {
		this.enumerationMode = mode;
	}

	public Mode getEnumerationMode() {
		return this.enumerationMode;
	}

	/**
	 * @return the requestForOptimizedEnumeration
	 */
	public boolean isRequestForOptimizedEnumeration() {
		return requestForOptimizedEnumeration;
	}

	/**
	 * @param requestForOptimizedEnumeration the requestForOptimizedEnumeration to set
	 */
	public void setRequestForOptimizedEnumeration(
			boolean requestForOptimizedEnumeration) {
		this.requestForOptimizedEnumeration = requestForOptimizedEnumeration;
	}

	/**
	 * @return the requestForTotalItemsCount
	 */
	public boolean isRequestForTotalItemsCount() {
		return requestForTotalItemsCount;
	}

	/**
	 * @param requestForTotalItemsCount the requestForTotalItemsCount to set
	 */
	public void setRequestForTotalItemsCount(boolean requestForTotalItemsCount) {
		this.requestForTotalItemsCount = requestForTotalItemsCount;
	}

	public Map<String, String> getNamespaceMap() {
		return namespaceMap;
	}

	public void setNamespaceMap(Map<String, String> map) {
		this.namespaceMap = map;
	}

	public String getEndTo() {
		return endTo;
	}

	public void setEndTo(String endTo) {
		this.endTo = endTo;
	}

	public long getExpires() {
		return expires;
	}

	public void setExpires(long expires) {
		this.expires = expires;
	}
	
}
