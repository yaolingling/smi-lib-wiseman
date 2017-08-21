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
 **$Log: EventingUtility.java,v $
 **Revision 1.6  2007/06/19 12:29:33  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.5  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: EventingUtility.java,v 1.6 2007/06/19 12:29:33 simeonpinder Exp $
 */
package com.sun.ws.management.eventing;

import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;

import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.eventing.FilterType;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.EnumerationMessageValues;

/** This class is meant to provide general utility functionality for
 *  Eventing message instances and all of their related extensions.
 *  To enable messages ready for submission, Management instances are 
 *  returned from some of the methods below. 
 * 
 *  The following default values are defined when newer values have not 
 *  been supplied:
 *  	UID_SCHEME = uuid:
 *  	DEFAULT_TIMEOUT = 30000
 *      ACTION = Enumeration.ENUMERATE_ACTION_URI 
 *  	FILTER_TYPE.DIALECT = XPATH
 *  	REPLY_TO = Anonymous URI
 *  
 * @author Nancy Beers
 */
public class EventingUtility {
	
	private static final Logger LOG = 
		Logger.getLogger(EventingUtility.class.getName());
	private static final org.xmlsoap.schemas.ws._2004._08.eventing.ObjectFactory evt_factory=
		new org.xmlsoap.schemas.ws._2004._08.eventing.ObjectFactory();
	
	public static Eventing buildMessage(Eventing existingEvent,
			EventingMessageValues settings) 
		throws SOAPException, JAXBException, DatatypeConfigurationException{
		
		if(existingEvent == null){//build default instances
			Management mgmt = ManagementUtility.buildMessage(null, settings);
		   existingEvent = new Eventing(mgmt);
		}
		if(settings ==null){//grab a default instance if 
			settings = EventingMessageValues.newInstance();
		}
		
		//Process the EventingConstants instance passed in.
		 //Processing ACTION for the message
		if (settings.getEventingMessageActionType() != null &&
				settings.getEventingMessageActionType().trim().length() > 0){
			existingEvent.setAction(settings.getEventingMessageActionType());
		} else {
			existingEvent.setAction(Eventing.SUBSCRIBE_ACTION_URI);
		}
		
	    //Process the EventingMessageValues instance passed in.
		 //Processing SUBSCRIBE action for the message
		if (settings.getEventingMessageActionType() == Eventing.SUBSCRIBE_ACTION_URI) {
			
			//Check to see if EventSinkDestination has been set
			String value = null;
			if(((value=settings.getEventSinkDestination())!=null)&&
					(value.trim().length()>0)){
			   //See whether a metadataUID has been added.
			   ReferenceParametersType params = null;
			   if(settings.getEventSinkReferenceParameterType()!=null){
				  params = settings.getEventSinkReferenceParameterType(); 
			   }
			   EndpointReferenceType notifyToEl = Management.createEndpointReference(
					   value.trim(), null, params, null, null);
			   settings.setNotifyTo(notifyToEl);
			}
			
			if (settings.getFilter() == null || settings.getFilter().length() <= 0) {
				existingEvent.setSubscribe(settings.getEndTo(), 
						settings.getDeliveryMode(), 
						settings.getNotifyTo(), 
					   settings.getExpires(), null);
			} else {
				final FilterType filter = Eventing.FACTORY.createFilterType();
		        filter.setDialect(settings.getFilterDialect());
		        filter.getContent().add(settings.getFilter());
				existingEvent.setSubscribe(settings.getEndTo(), settings.getDeliveryMode(), settings.getNotifyTo(), 
						   settings.getExpires(), filter);
				
			}
		}//Processing SubscribeResponse Action. 
		else if (settings.getEventingMessageActionType() == Eventing.SUBSCRIBE_RESPONSE_URI) {
			existingEvent.setSubscribeResponse(settings.getNotifyTo(), settings.getExpires());
		} else if (settings.getEventingMessageActionType() == Eventing.RENEW_ACTION_URI) {
			existingEvent.setRenew(settings.getExpires());
		} else if (settings.getEventingMessageActionType() == Eventing.RENEW_RESPONSE_URI) {
			existingEvent.setRenewResponse(settings.getExpires());
		} else if (settings.getEventingMessageActionType() == Eventing.GET_STATUS_ACTION_URI) {
			existingEvent.setGetStatus();
		} else if (settings.getEventingMessageActionType() == Eventing.GET_STATUS_RESPONSE_URI) {
			existingEvent.setGetStatusResponse(settings.getExpires());
		} else if (settings.getEventingMessageActionType() == Eventing.UNSUBSCRIBE_ACTION_URI) {
			existingEvent.setUnsubscribe();
		} else if (settings.getEventingMessageActionType() == Eventing.SUBSCRIPTION_END_ACTION_URI) {
			existingEvent.setSubscriptionEnd(settings.getEndTo(), settings.getStatus(), settings.getReason());
		}
		
		 //Processing ReplyTo
		if((settings.getReplyTo()!=null)&&
		    settings.getReplyTo().trim().length()>0){
			existingEvent.setReplyTo(settings.getReplyTo());
		}else{
			existingEvent.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
		}
		
		 //Processing MessageId component
		if((settings.getUidScheme()!=null)&&
				(settings.getUidScheme().trim().length()>0)){
			existingEvent.setMessageId(settings.getUidScheme() +
				   UUID.randomUUID().toString());
		}else{
			existingEvent.setMessageId(EnumerationMessageValues.DEFAULT_UID_SCHEME +
			  UUID.randomUUID().toString());
		}
		
		
		if (settings.getNamespaceMap() != null &&
				settings.getNamespaceMap().size() > 0){
			existingEvent.addNamespaceDeclarations(settings.getNamespaceMap());
		} 
		
		return existingEvent;
	}
}
