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
 **$Log: EventSourceInterface.java,v $
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
 *
 * $Id: EventSourceInterface.java,v 1.3 2007/06/19 12:29:33 simeonpinder Exp $
 */
package com.sun.ws.management.framework.eventing;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;

import com.sun.ws.management.Management;
import com.sun.ws.management.server.Handler;
import com.sun.ws.management.server.HandlerContext;

/** This interface defines the required methods for a 
 * EventSource instance.  An Event Source
 * must support SUBSCRIBE.  How an Event Source and it's
 * associated Subscription Manager communicate crucial information
 * when they are VM different, is currently ambiguously defined by 
 * the specifications. 
 * 
 * Provide implementations for:
 * 	 isAlsoTheSubscriptionManager()
 * 	 getSubscriptionManager()
 * 
 * @author Simeon
 */
public interface EventSourceInterface extends Handler {
	
	/**Fundamental method of an Event Source. See WS-Eventing
	 * for more details about how this should be implemented.
	 * 
	 * @param context
	 * @param eventRequest
	 * @param eventResponse
	 * @return
	 * @throws SOAPException
	 * @throws JAXBException
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 */
	public Management subscribe(String resource,
			HandlerContext context, Management request,
			Management response) throws JAXBException, SOAPException,
			DatatypeConfigurationException, IOException;
    
    /* Implementors should consider supporting this item to allow other entities
     * the ability to create events upon request from this entity.
     */
    public void create(HandlerContext context, Management request, 
    		Management response) throws Exception;
    
    //Subscription Manager interaction details.
    /** Flag indicating whether this EventSource is also the
     *  SubscriptionManager instance as well. No soap message
     *  communication with SubscriptionManager instance necessary
     *  if the Event Source and Subscription Manager are the same 
     *  handler. 
     * 
     * @return boolean flag indicating the above status.
     */
    boolean isAlsoTheSubscriptionManager();

    /**This is required to indicate how to 
     * contact the remote Subscription Manager. The
     * Management instance returned is expected to be
     * a Wiseman Metadata artifact with all required
     * Addressing information already populated.
     * 
     * @return
     * @throws SOAPException
     * @throws JAXBException
     * @throws DatatypeConfigurationException
     * @throws IOException
     */
    public Management getMetadataForEventSource() throws SOAPException, 
    JAXBException, DatatypeConfigurationException, IOException;

    /**This is required to indicate how  
     * the remote Subscription Manager is to communicate with the
     * remote EventSource that it is managing subscriptions for. The
     * Management instance returned is expected to be
     * a Wiseman Metadata artifact with all required
     * Addressing information already populated.  Most times the
     * EventSource is already annotated with the information above,
     * so you just need to retrieve the attached annotation and 
     * run it through the AnnotationProcessing api.  See 
     * SampleEventSourceHandler for an example.
     * 
     * @return
     * @throws SOAPException
     * @throws JAXBException
     * @throws DatatypeConfigurationException
     * @throws IOException
     */
    public Management getMetadataForSubscriptionManager() throws SOAPException, 
    JAXBException, DatatypeConfigurationException, IOException;

}
