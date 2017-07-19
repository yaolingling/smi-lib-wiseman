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
 **Revision 1.4  2007/06/19 12:29:33  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.3  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: EventingMessageValues.java,v 1.5 2008-01-17 15:19:09 denis_rachal Exp $
 */
package com.sun.ws.management.eventing;

import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;

import com.sun.ws.management.ManagementMessageValues;
import com.sun.ws.management.enumeration.EnumerationExtensions.Mode;

/**This class is meant to be a container for constants
 * that are used or applied to an Eventing message.
 * No complicated methods or logic should be present.
 * 
 * See following for original settings as ManagemetMessageConstant
 * values apply.
 * {@link ManagementMessageValues}
 *  
 * @author Nancy Beers
 */
public class EventingMessageValues extends ManagementMessageValues {
	
	//uri to distinguish wiseman eventing specific communication. 
    public static final String EVENTING_COMMUNICATION_ACTION_URI = 
    	"http://wiseman.dev.java.net/ws/eventing/communication";

    public static final String EVENT_SOURCE_NODE_NAME = "EventSource";
    public static final String EVENT_SINK_NODE_NAME = "EventSink";
    public static final String EVENT_PREFIX = "wec";
    public static final String EVENT_SOURCE_DESC_ATTR_NAME = "info";
    public static final String EVENT_SOURCE_ID_ATTR_NAME = "evt-src-id";
    public static final String EVENTING_CREATIION_TYPE = "CreationType";
    public static final String EVENTING_EXPIRATION_TYPE = "ExpirationType";

    //QName used during customized eventing message processing/generation
    public static final QName EVENTING_CREATION_TYPES = new 
    QName(EVENTING_COMMUNICATION_ACTION_URI,
    		EVENTING_CREATIION_TYPE,EVENT_PREFIX);
    
    //QName used during customized eventing message processing/generation
    public static final QName EVENTING_COMMUNICATION_CONTEXT_ID = new 
    QName(EVENTING_COMMUNICATION_ACTION_URI,
    		"ContextId",EVENT_PREFIX);
    //QName used during customized eventing message processing/generation
    public static final QName EVENT_SOURCE_ID = new 
    QName(EVENTING_COMMUNICATION_ACTION_URI,
    		EVENT_SOURCE_NODE_NAME,EVENT_PREFIX);
    
    //QName used during EventingSourceDescription
    public static final QName EVENT_CONTEXT_INFO = new 
    QName(EVENTING_COMMUNICATION_ACTION_URI,
    		"EventSourceDescription",EVENT_PREFIX);
    
    //QName used during NewSubscriber
    public static final QName EVENT_SINK = new 
    QName(EVENTING_COMMUNICATION_ACTION_URI,
    		EVENT_SINK_NODE_NAME,EVENT_PREFIX);
    
    //QName used during ExpirationType communication
    public static final QName EVENT_EXPIRATION = new 
    	QName(EVENTING_COMMUNICATION_ACTION_URI,
    			EVENT_SINK_NODE_NAME,EVENT_PREFIX);
    
    public static final String SUBSCRIPTION_SOURCE = "SUBSCRIPTION_SOURCE";
    public static final String EVENT_SINK_NAME = "EVENT_SINK";
    public static enum CreationTypes { SUBSCRIPTION_SOURCE, NEW_SUBSCRIBER, NEW_EVENT,EVENT_SINK};
    
    //the package that generated JAXB types is put into, or used during binding/unbinding
    public static String TEST_EVENT_PACKAGE ="com.hp.examples.ws.wsman.test_event";
    //used in generation of message ids.
    public static final String UUID_SCHEME = "uuid:";
    //Custom Event action.
    public static final String CUSTOM_ACTION_URI = "http://examples.hp.com/ws/wsman/test-event/NewEvent";
    public static final String CUSTOM_RESPONSE_URI = "http://examples.hp.com/ws/wsman/test-event/NewEventResponse";
    
    //////#########################################################################################
	public static final String DEFAULT_EVENTING_MESSAGE_ACTION_TYPE = Eventing.SUBSCRIBE_ACTION_URI;
	public static final String DEFAULT_UID_SCHEME=ManagementMessageValues.DEFAULT_UID_SCHEME;
	public static final String DEFAULT_FILTER = "";
	public static final String DEFAULT_FILTER_DIALECT = com.sun.ws.management.xml.XPath.NS_URI;
	public static final Map<String, String> DEFAULT_NS_MAP = null;
	public static final String DEFAULT_STATUS = "";
	public static final String DEFAULT_REASON = "";

	public static final String[] DEFAULT_CUSTOM_XML_BINDINGS = {};
	public static final Mode DEFAULT_ENUMERATION_MODE = null;
	private static final String DEFAULT_EVENT_SOURCE_ID = "";
	private static final String DEFAULT_EVENT_SINK_ID = "";
	private static final EndpointReferenceType DEFAULT_END_TO = null;
	private static final EndpointReferenceType DEFAULT_NOTIFY_TO = null;
	private static final String DEFAULT_DELIV_MODE = Eventing.PUSH_DELIVERY_MODE;
	
	
	protected EndpointReferenceType endTo = DEFAULT_END_TO;
	protected String deliveryMode = DEFAULT_DELIV_MODE;
	protected EndpointReferenceType notifyTo = DEFAULT_NOTIFY_TO;
	
	protected String eventingMessageActionType = DEFAULT_EVENTING_MESSAGE_ACTION_TYPE;
	private static final String DEFAULT_EVENT_SINK_DESTINATION = "";
	private static final EndpointReferenceType DEFAULT_SUBSCRIPTION_MANAGER_EPR = null;
	
	public static final String DEFAULT_EVT_SINK_UUID_SCHEME = "evt-snk-uid:";
	public static final long DEFAULT_SUBSCRIPTION_TIMEOUT=1000*60*10;
	public static final long DEFAULT_SUBSCRIPTION_TIMEOUT_FLOOR=1000*60*2;
	private static Duration defaultExpires =null;
	static{
		try{
	      defaultExpires = DatatypeFactory.newInstance().newDuration(DEFAULT_SUBSCRIPTION_TIMEOUT);
		}catch(Exception ex){
			//Eat the exception to avoid cryptic init failures.
			ex.printStackTrace();
		}
	}
	protected String expires = defaultExpires.toString();
	private String uidScheme = DEFAULT_UID_SCHEME;
	private String filter = DEFAULT_FILTER;
	private String filterDialect = DEFAULT_FILTER_DIALECT;
	private Map<String, String> namespaceMap = DEFAULT_NS_MAP;
	private String status = DEFAULT_STATUS;
	private String reason = DEFAULT_REASON;
	private String replyTo = DEFAULT_REPLY_TO;
	private String identifier = null;
	private String[] customXmlBindingPackageList =DEFAULT_CUSTOM_XML_BINDINGS;
	private ReferenceParametersType referenceParameterType = null;
	//TODO: change this name as it's not intuitive.
	private String eventSourceUid = DEFAULT_EVENT_SOURCE_ID;
	private String eventSinkUid = DEFAULT_EVENT_SINK_ID;
	private String eventSinkDestination = DEFAULT_EVENT_SINK_DESTINATION;
	private String eventSinkUidScheme = DEFAULT_EVT_SINK_UUID_SCHEME;
	private ReferenceParametersType eventSinkReferenceParameterType = null;
	private EndpointReferenceType subscriptionManagerEpr = DEFAULT_SUBSCRIPTION_MANAGER_EPR;
	
	public String getUidScheme() {
		return uidScheme;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getFilterDialect() {
		return filterDialect;
	}

	public void setFilterDialect(String filterDialect) {
		this.filterDialect = filterDialect;
	}
	
	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}
	
	public String getIdentifier() {
		return this.identifier;
	}

	public void setUidScheme(String uidScheme) {
		this.uidScheme = uidScheme;
	}

	/** Instance has all the default values set. 
	 * 
	 * @throws SOAPException
	 */
	public EventingMessageValues() throws SOAPException{
	}
	
	public static EventingMessageValues newInstance() throws SOAPException{
		return new EventingMessageValues();
	}

	public String getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(String deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public EndpointReferenceType getEndTo() {
		return endTo;
	}

	public void setEndTo(EndpointReferenceType endTo) {
		this.endTo = endTo;
	}

	public String getExpires() {
		return expires;
	}

	public void setExpires(String expires) {
		this.expires = expires;
	}

	public EndpointReferenceType getNotifyTo() {
		return notifyTo;
	}

	public void setNotifyTo(EndpointReferenceType notifyTo) {
		this.notifyTo = notifyTo;
	}

	public String getEventingMessageActionType() {
		return eventingMessageActionType;
	}

	public void setEventingMessageActionType(String eventingMessageActionType) {
		this.eventingMessageActionType = eventingMessageActionType;
	}

	public Map<String, String> getNamespaceMap() {
		return namespaceMap;
	}

	public void setNamespaceMap(Map<String, String> namespaceMap) {
		this.namespaceMap = namespaceMap;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String[] getCustomXmlBindingPackageList() {
		return customXmlBindingPackageList;
	}

	public void setCustomXmlBindingPackageList(String[] customXmlBindingPackageList) {
		this.customXmlBindingPackageList = customXmlBindingPackageList;
	}

	public String getEventSinkDestination() {
		return eventSinkDestination;
	}

	public void setEventSinkDestination(String eventSinkDestination) {
		this.eventSinkDestination = eventSinkDestination;
	}

	public ReferenceParametersType getEventSinkReferenceParameterType() {
		return eventSinkReferenceParameterType;
	}

	public void setEventSinkReferenceParameterType(
			ReferenceParametersType eventSinkReferenceParameterType) {
		this.eventSinkReferenceParameterType = eventSinkReferenceParameterType;
	}

	public String getEventSinkUid() {
		return eventSinkUid;
	}

	public void setEventSinkUid(String eventSinkUid) {
		this.eventSinkUid = eventSinkUid;
	}

	public String getEventSinkUidScheme() {
		return eventSinkUidScheme;
	}

	public void setEventSinkUidScheme(String eventSinkUidScheme) {
		this.eventSinkUidScheme = eventSinkUidScheme;
	}

	public String getEventSourceUid() {
		return eventSourceUid;
	}

	public void setEventSourceUid(String eventSourceUid) {
		this.eventSourceUid = eventSourceUid;
	}

	public ReferenceParametersType getReferenceParameterType() {
		return referenceParameterType;
	}

	public void setReferenceParameterType(
			ReferenceParametersType referenceParameterType) {
		this.referenceParameterType = referenceParameterType;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public EndpointReferenceType getSubscriptionManagerEpr() {
		return subscriptionManagerEpr;
	}

	public void setSubscriptionManagerEpr(
			EndpointReferenceType subscriptionManagerEpr) {
		this.subscriptionManagerEpr = subscriptionManagerEpr;
	}

}
