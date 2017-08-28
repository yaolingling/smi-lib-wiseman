/*
 * Copyright 2006 Sun Microsystems, Inc.
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
 **$Log: SubscriptionManagerInterface.java,v $
 **Revision 1.3  2007/06/19 12:29:33  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.2  2007/05/30 20:30:31  nbeers
 **Add HP copyright header
 **
 **
 * $Id: SubscriptionManagerInterface.java,v 1.3 2007/06/19 12:29:33 simeonpinder Exp $
 *
 */
package com.sun.ws.management.framework.eventing;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;

import com.sun.ws.management.Management;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.Handler;

/** This interface defines the required methods for a 
 * SubscriptionManagerInterface instance.  A subscription manager
 * must support RENEW, UNSUBSCRIBE and optionally
 * may support SUBSCRIPTIONEND.  The GETSTATUS message is 
 * explicitly NOT RECOMMENDED. 
 * 
 * @author Simeon
 */
public interface SubscriptionManagerInterface extends Handler {
	/** By extending Handler this requires that the instance be
	 *  a Wiseman handler.  
	 */
	
	String getSubscriptionManagerAddress();
    String getSubscriptionManagerResourceURI();
    
    /**This method is encouraged so that a remote Event Source
     * can send Transfer.Create requests to create/register 
     * new EventSources and new Event Sink(Subscribers) to this
     * SubscriptionManager instance.  See eventsubman_Handler
     * for a default implementation that is a suggestion for 
     * how such a resource could be built.  
     * 
     * @param context Handler context if useful
     * @param request Management message
     * @param response Management message
     */
    Management create(HandlerContext context,Management request, 
    		Management response) throws SOAPException, 
			JAXBException, DatatypeConfigurationException ;
    
    /**Fundamental method for the remote SubscriptionManager.
     * Should handle the task of registering a subscriber/eventsink
     * from this Subscription Manager.
     * 
     * @param context Handler context if useful
     * @param request Management message
     * @param response Management message
     */
	Management unsubsubscribe(HandlerContext context,Management request, 
	Management response);
	
}
