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
 **$Log: MetadataUtility.java,v $
 **Revision 1.7  2007/06/19 12:29:34  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.6  2007/05/30 20:30:30  nbeers
 **Add HP copyright header
 **
 **
 *
 * $Id: MetadataUtility.java,v 1.7 2007/06/19 12:29:34 simeonpinder Exp $
 */
package com.sun.ws.management.mex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.w3._2003._05.soap_envelope.Fault;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.eventing.DeliveryType;
import org.xmlsoap.schemas.ws._2004._08.eventing.Subscribe;
import org.xmlsoap.schemas.ws._2004._09.mex.Metadata;
import org.xmlsoap.schemas.ws._2004._09.mex.MetadataSection;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingMessageValues;
import com.sun.ws.management.eventing.EventingMessageValues.CreationTypes;
import com.sun.ws.management.framework.eventing.EventSourceInterface;
import com.sun.ws.management.framework.eventing.SubscriptionManagerInterface;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.identify.IdentifyUtility;
import com.sun.ws.management.metadata.annotations.AnnotationProcessor;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.transfer.TransferExtensions;
import com.sun.ws.management.transfer.TransferUtility;
import com.sun.ws.management.transport.BasicAuthenticator;
import com.sun.ws.management.transport.HttpClient;
import com.sun.xml.fastinfoset.sax.Properties;

/** This class is meant to provide general utility functionality for
 *  Management instances and all of their related extensions.
 *
 * @author Simeon
 */
public class MetadataUtility extends ManagementUtility {

	//These values are final and static so that they can be uniformly used by many classes
	private static final Logger LOG = Logger.getLogger(MetadataUtility.class.getName());

	/** This method takes a GetResponse Management instance containing a
	 *  a MetaDataExchange element.  An array of Management instances located
	 *  is returned in response.
	 *
	 * @param metaDataGetResponse
	 * @return array of Management instances
	 */
	public static Management[] extractEmbeddedMetaDataElements(Management metaDataGetResponse){
		Management[] locatedMetaDataElements = null;
		ArrayList<Management> located = new ArrayList<Management>();

   		//Retrieve the MetaData response to build JAXB type
   		SOAPBody body = metaDataGetResponse.getBody();

   		if((body!=null)&&(body.getFirstChild()!=null)){
	   	 //Normal processing to create/retrieve the Metadata object
	   	 Node metaDataNode = body.getFirstChild();

			try {
			 //unmarshall the Metadata node content
			 Object bound = metaDataGetResponse.getXmlBinding().unmarshal(metaDataNode);
			 if((bound!=null) && (bound instanceof Metadata)){
				 Metadata ob = (Metadata)bound;

				//Parse the MetadataSections that exist
				List<MetadataSection> metaDataSections =
					ob.getMetadataSection();

				if(metaDataSections!=null){
				 for (Iterator iter = metaDataSections.iterator(); iter.hasNext();) {
					MetadataSection element = (MetadataSection) iter.next();
					if((element.getDialect()!=null)&&
							(element.getDialect().equals(AnnotationProcessor.NS_URI))){
						Management instance = new Management();
						//Now parse the Dialect specif component.
						instance = AnnotationProcessor.populateMetadataInformation(element,
								instance);
						located.add(instance);
					}
				}//end of for loop.
			 }//end of if metaDataSections exist
		    }
   		   }catch (JAXBException e) {
   			  //log and eat the exception
   			LOG.log(Level.FINE, "JAXBException occurred:"+e.getMessage());
   		   } catch (SOAPException e) {
			  //log and eat the exception
  			LOG.log(Level.FINE, "SOAPException occurred:"+e.getMessage());
   		   }
		}

   		//Now populate the return array.
   		locatedMetaDataElements = new Management[located.size()];
   		System.arraycopy(located.toArray(), 0,
   				locatedMetaDataElements, 0, located.size());

	   return locatedMetaDataElements;
	}

	public static Management[] getExposedMetadata(String wisemanServerAddress,long timeout)
		throws SOAPException, IOException, JAXBException, DatatypeConfigurationException{
		long timeoutValue = 30000;
		if(timeout>timeoutValue){
			timeoutValue = timeout;
		}
		Management[] metaDataValues = null;
		loadServerAccessCredentials(null);

		//Make identify request to the Wiseman server
        final Identify identify = new Identify();
        identify.setIdentify();
        //Send identify request
        final Addressing response =
        	HttpClient.sendRequest(identify.getMessage(),
        			wisemanServerAddress);

        //Parse the identify response
        final Identify id = new Identify(response);
        final SOAPElement idr = id.getIdentifyResponse();
        SOAPElement el = IdentifyUtility.locateElement(id,
        		AnnotationProcessor.META_DATA_RESOURCE_URI);

        //retrieve the MetaData ResourceURI
        String resUri=el.getTextContent();
        el = IdentifyUtility.locateElement(id,
        		AnnotationProcessor.META_DATA_TO);

        //retrieve the MetaData To/Destination
        String metTo=el.getTextContent();

     //exercise the Enumeration annotation mechanism
        //############ REQUEST THE LIST OF METADATA AVAILABLE ######################
 	   //Build the GET request to be submitted for the metadata
        Management m = TransferUtility.createMessage(metTo, resUri,
        		Transfer.GET_ACTION_URI, null, null, timeoutValue, null);

          //############ PROCESS THE METADATA RESPONSE ######################
          //Parse the getResponse for the MetaData
          final Addressing getResponse = HttpClient.sendRequest(m);
        Management mResp = new Management(getResponse);

		metaDataValues =
			MetadataUtility.extractEmbeddedMetaDataElements(mResp);

		return metaDataValues;
	}

	public static void loadServerAccessCredentials(Properties properties){
		String wsmanDest="wsman.dest";
		String wsmanUser="wsman.user";
		String wsmanPassword="wsman.password";
	    String wsmanBasicAuthenticationEnabled="wsman.basicauthentication";
//        <jvmarg value="-Dwsman.dest=http://localhost:8080/wsman/" />
//        <jvmarg value="-Dwsman.user=wsman" />
//        <jvmarg value="-Dwsman.password=secret" />
//        <jvmarg value="-Dwsman.basicauthentication=true" />
	    String key = null;
	    if((key=System.getProperty(wsmanDest))==null){
	    	System.setProperty(wsmanDest, "http://localhost:8080/wsman/");
	    }
	    if((key=System.getProperty(wsmanUser))==null){
	    	System.setProperty(wsmanUser, "wsman");
	    }
	    if((key=System.getProperty(wsmanPassword))==null){
	    	System.setProperty(wsmanPassword, "secret");
	    }
	    if((key=System.getProperty(wsmanBasicAuthenticationEnabled))==null){
	        System.setProperty(wsmanBasicAuthenticationEnabled, "true");
	    }

	    final String basicAuth = System.getProperty("wsman.basicauthentication");
        if ("true".equalsIgnoreCase(basicAuth)) {
        	HttpClient.setAuthenticator(new BasicAuthenticator());
//            HttpClient.setAuthenticator(new SimpleHttpAuthenticator());
        }

	}

	//EVENTING METHODS
	public static String registerEventSourceWithSubscriptionManager(EventSourceInterface
			eventSource,boolean logException,boolean throwException) throws
	SOAPException, JAXBException, DatatypeConfigurationException, IOException{
		String eventSourceUid = null;

		if(eventSource==null){
			String msg = "The EventSourceInterface instance was NULL. This is not allowed.";
			if(logException){
				LOG.severe(msg);
			}
			if(throwException){
			  throw new IllegalArgumentException(msg);
			}
			return eventSourceUid;
		}

		//Retrieve the SubscriptionManager details
		Management subManagerData = null;

		//locate the subscriptionManager instance
		if(!eventSource.isAlsoTheSubscriptionManager()){
			subManagerData = eventSource.getMetadataForSubscriptionManager();
			//component ananlysis.
			//TODO: insert check for ADDRESSING.TO as this must be set. All others variable.
			if(subManagerData==null){
				String msg="SubscriptionManager metadata is null. Unable to proceed.";
				if(logException){
					LOG.severe(msg);
				}
				if(throwException){
				  throw new IllegalArgumentException(msg);
				}
				return eventSourceUid;
			}

			//Add the specific headers to the message to ensure correct processing.
			subManagerData.addHeaders(Management.createReferenceParametersType(
					EventingMessageValues.EVENTING_CREATION_TYPES,
					CreationTypes.SUBSCRIPTION_SOURCE.name()));
	    	//Retrieve the Event Source details
			Management evtSrcDetails =
				eventSource.getMetadataForEventSource();

			//Continue to add required elements to indicate correct processing path..
			 //Locate the MetaDataUID for the event source(should be unique across server)
			SOAPElement evtSrcMetDataId = ManagementUtility.locateHeader(
					evtSrcDetails.getHeaders(),
					AnnotationProcessor.RESOURCE_META_DATA_UID);
			if(evtSrcMetDataId!=null){
			  String evtMetadata = evtSrcMetDataId.getTextContent();
			  if((evtMetadata!=null)&&!(evtMetadata.trim().equals(""))){
				//Located an event source requested ID. Add as header
			    subManagerData.addHeaders(Management.createReferenceParametersType(
				 EventingMessageValues.EVENTING_COMMUNICATION_CONTEXT_ID,
				 evtMetadata));
			  }
			}

			//set correct action
			subManagerData.setAction(Transfer.CREATE_ACTION_URI);
			//set the reply to to be anonymous
			subManagerData.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);

			//populate the other required elements of message
			subManagerData = ManagementUtility.buildMessage(null,
//					subManagerData, true);
					subManagerData, false);

			//Reset the message to new UID to prevent message misdirection???
			subManagerData.setMessageId(EventingMessageValues.DEFAULT_EVT_SINK_UUID_SCHEME+
					UUID.randomUUID());

	        final Addressing response = HttpClient.sendRequest(subManagerData);
	        if (response.getBody().hasFault())
	        {
				String msg="There was a problem communicating with the remote SubscriptionManager:\n";
				Fault fault = response.getFault();
				if(fault.getDetail()!=null){
					if(fault.getDetail().getAny()!=null){
						for(Object det:fault.getDetail().getAny()){
							msg+=det+"\n";
						}
					}
				}
				if(logException){
					LOG.severe(msg);
				}
				if(throwException){
				  throw new RuntimeException(msg);
				}
	        }else{
	        	//extract the returned selectorSet
	        	Management eventSourceRegistrationResp = new Management(response);

	          Map<String,String> selectors=
	        	  ManagementUtility.extractSelectors(eventSourceRegistrationResp);
	          if(selectors.size()>0){
	        	if(selectors.containsKey(EventingMessageValues.EVENT_SOURCE_ID_ATTR_NAME)){
	        		eventSourceUid = selectors.get(
	        			EventingMessageValues.EVENT_SOURCE_ID_ATTR_NAME);
	        	}
	          }
	        }
		}

		return eventSourceUid;
	}

	public static Management registerEventSinkWithSubscriptionManager(
			EventSourceInterface eventSource,Management message, boolean logException,
			boolean throwException) throws JAXBException, SOAPException, 
			DatatypeConfigurationException, IOException {
		//The reference to be returned
	    Management subManagerData = null;

	    //locate the subscriptionManager instance
	  if(!eventSource.isAlsoTheSubscriptionManager()){
	    	subManagerData = eventSource.getMetadataForSubscriptionManager();
			if(subManagerData==null){
				String msg="SubscriptionManager metadata is null. Unable to proceed.";
				if(logException){
					LOG.severe(msg);
				}
				if(throwException){
				  throw new IllegalArgumentException(msg);
				}
				return subManagerData;
			}

			//Now insert the relevant parameters to tell the SubscriptionManager how to process
				//Indicate that this is a NEW_SUBSCRIBER packet
			subManagerData.addHeaders(Management.createReferenceParametersType(
					EventingMessageValues.EVENTING_CREATION_TYPES,
					CreationTypes.NEW_SUBSCRIBER.name()));
				Management evtSrcDetails =
					eventSource.getMetadataForEventSource();
			//Indicate which EventSource this message comes from
			SOAPElement evtSrcMetDataId = ManagementUtility.locateHeader(
					evtSrcDetails.getHeaders(),
					AnnotationProcessor.RESOURCE_META_DATA_UID);
			String srcMetadataKey = null;
			if(evtSrcMetDataId!=null){
				srcMetadataKey =evtSrcMetDataId.getTextContent();
			}
			
			//Now take the content of the incoming message
				//This is event sink lookup.
			if((srcMetadataKey!=null)&&
			      (srcMetadataKey.trim().length()>0)){
			  //Now that we have the metadata for the EventSource and can send mesg
				//lookup the EventSink info passed in.
			  String evtMetadata = srcMetadataKey;
			  String eventSrcId = null;
//				String evtMetadata = null;
				//Dig into subscribe message sent
				TransferExtensions create=new TransferExtensions(message);
				Node node =create.getBody().getFirstChild();
				Object jaxbObj = message.getXmlBinding().unmarshal(node);
				if((jaxbObj!=null)&&(jaxbObj instanceof Subscribe)){
				  Subscribe subscribe = (Subscribe) jaxbObj;
				  DeliveryType del = null;
				  if((del=subscribe.getDelivery())!=null){
					 List<Object> contents = null; 
					 if(((contents=del.getContent())!=null)&&
							 (!contents.isEmpty())){
						for(Object obj:contents){
						   Class type = obj.getClass();
						   if (JAXBElement.class.equals(type)) {
							final JAXBElement<Object> element = 
								(JAXBElement<Object>) obj;
							final QName name = element.getName();
							final Object item = element.getValue();
							if (item instanceof EndpointReferenceType) {
								final EndpointReferenceType eprT = 
									(EndpointReferenceType) item;
								if((eprT!=null)&&(Eventing.NOTIFY_TO.equals(name))) {
									if(eprT.getAddress()!=null){
										evtMetadata = eprT.getAddress().getValue();
									}
									if(eprT.getReferenceParameters()!=null){
										ReferenceParametersType refPars = 
											eprT.getReferenceParameters();
										if(refPars.getAny()!=null){
											for(Object ref :refPars.getAny()){
											  if(ref instanceof Node){
												Node nod = (Node) ref;
												if(nod.getLocalName().equals(
													AnnotationProcessor.
													RESOURCE_META_DATA_UID.
													getLocalPart())){
													evtMetadata = nod.getTextContent();
												}
											  }
											  else if(ref instanceof Element){
												 Element nod = (Element) ref;
												 if(nod.getLocalName().equals(
													AnnotationProcessor.
													RESOURCE_META_DATA_UID.
													getLocalPart())){
												  evtMetadata = nod.getTextContent();
												 }
											  }
											  else{System.out.println("#### Not instance of node/elem:");};
											}

										}
									}else{System.out.println("#### EPR has not rep params");}
								}
							}//end of if EPR
						   }//End of if JAXBElement check
						}
					 }
				  }
				}
				
				if(evtMetadata!=null){
				 //put the event sink id in the subscriptionManager response message. Is UID	
				 subManagerData.addHeaders(
					Management.createReferenceParametersType(
					EventingMessageValues.EVENTING_COMMUNICATION_CONTEXT_ID,
				 evtMetadata));
				 //put the event source id here.  Useful info for unsubscribe.
			       subManagerData.addHeaders(
					  Management.createReferenceParametersType(
					Eventing.IDENTIFIER,
					srcMetadataKey));
			     subManagerData.setAction(Transfer.CREATE_ACTION_URI);
			     subManagerData.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
				}
			}

			//build the message to be sent
			subManagerData.setMessageId(EventingMessageValues.DEFAULT_UID_SCHEME+UUID.randomUUID());
			subManagerData = ManagementUtility.buildMessage(null,
	//				subManagerData, true);
					subManagerData, false);
			//insert the body of the message from the message passed in.
			if((message!=null)&&(message.getBody()!=null)){
				subManagerData.getBody().removeContents();
				subManagerData.getBody().addDocument(
					   message.getBody().extractContentAsDocument());
			}
			subManagerData.setMessageId(
					EventingMessageValues.DEFAULT_EVT_SINK_UUID_SCHEME+UUID.randomUUID());

		//submit the request to the subscription manager
        final Addressing response = HttpClient.sendRequest(subManagerData);

        if (response.getBody().hasFault()){
			String msg="There was a problem communicating with the remote SubscriptionManager:\n";
			Fault fault = response.getFault();
			if(fault.getDetail()!=null){
				if(fault.getDetail().getAny()!=null){
					for(Object det:fault.getDetail().getAny()){
						msg+=det+"\n";
					}
				}
			}
			if(logException){
				LOG.severe(msg);
			}
			if(throwException){
			  throw new RuntimeException(msg);
			}
        }else{
 			subManagerData = new Management(response);
        }
	  }//End of is NOT also a subscription manager loop.

	  subManagerData.setReplyTo(Addressing.ANONYMOUS_ENDPOINT_URI);
	 return subManagerData;
	}

	public static Management unRegisterEventSinkFromSubscriptionManager(
			SubscriptionManagerInterface manager, Management request, 
			boolean logException,
			boolean throwException) throws SOAPException, JAXBException, 
			DatatypeConfigurationException, IOException{
		Management response = null;
		if(manager==null){
			String msg="SubscriptionManager metadata is null. Unable to proceed.";
			if(logException){
				LOG.severe(msg);
			}
			if(throwException){
			  throw new IllegalArgumentException(msg);
			}
			return null;
		}
		//begin processing...
		if(request!=null){
			SOAPElement evtSinkSoapEl = ManagementUtility.locateHeader(
					request.getHeaders(), 
					EventingMessageValues.EVENT_SINK);
			String eventSink=null;
			SOAPElement evtSrcSoapEl = ManagementUtility.locateHeader(request.getHeaders(), 
					EventingMessageValues.EVENTING_COMMUNICATION_CONTEXT_ID);
			String eventSource = null;
			if((evtSinkSoapEl==null)||(evtSrcSoapEl==null)||
				((eventSink =evtSinkSoapEl.getTextContent())==null)||
				((eventSource =evtSrcSoapEl.getTextContent())==null)||
				(eventSink.trim().equals("")||
				(eventSource.trim().equals("")))){
				String msg="The Event Source or Event Sink headers cannot be null.";
				throw new IllegalArgumentException(msg);
			}
			//pipe the request off to the EventSource
			  //attempt to get MetadataMessage for EventSource
			  Management eventSourceMessage = 
				  AnnotationProcessor.findAnnotatedResourceByUID(
						  eventSource, request.getTo());
			  //finish fill out message request
			  eventSourceMessage.setAction(Transfer.DELETE_ACTION_URI);
			  //run through utility to populate remaining messages
			  eventSourceMessage= ManagementUtility.buildMessage(
					  eventSourceMessage, 
					  null);
			  //Add the two necessary headers to event source
			  eventSourceMessage.addHeaders(
					  Management.createReferenceParametersType(
							  EventingMessageValues.EVENT_SINK, eventSink));
			  eventSourceMessage.addHeaders(
					  Management.createReferenceParametersType(
							  EventingMessageValues.EVENTING_COMMUNICATION_CONTEXT_ID, 
							  eventSource));
			  final Addressing unsubResp = HttpClient.sendRequest(eventSourceMessage);
			  if(unsubResp.getFault()!=null){
				String msg="";
				if(unsubResp.getFault().getDetail()!=null){
				 for(Object ob:unsubResp.getFault().getDetail().getAny()){
					msg+=ob+"\n";
				 }
				}
				if(logException){
					LOG.severe(msg);
				}
				if(throwException){
				  throw new IllegalArgumentException(msg);
				}
				return null;
			  }
			  
		}
		return response;
	}
}
