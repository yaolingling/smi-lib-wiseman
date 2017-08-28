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
 **$Log: AnnotationProcessor.java,v $
 **Revision 1.9  2007/06/20 09:22:40  simeonpinder
 **-1.0 binary cleanup and 2 more descriptive error messages.
 **
 **Revision 1.8  2007/06/19 12:29:34  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.7  2007/06/04 06:25:13  denis_rachal
 **The following fixes have been made:
 **
 **   * Moved test source to se/test/src
 **   * Moved test handlers to /src/test/src
 **   * Updated logging calls in HttpClient & Servlet
 **   * Fxed compiler warning in AnnotationProcessor
 **   * Added logging files for client junit tests
 **   * Added changes to support Maven builds
 **   * Added JAX-WS libraries to CVS ignore
 **
 **Revision 1.6  2007/05/30 20:30:25  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: AnnotationProcessor.java,v 1.9 2007/06/20 09:22:40 simeonpinder Exp $
 */
package com.sun.ws.management.metadata.annotations;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.soap.SOAPFaultException;

import net.java.dev.wiseman.schemas.metadata.messagetypes.InputType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.MessageDefinitions;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OperationNodeType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OperationsType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OutputType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.SchemaType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.SchemasType;

import org.dmtf.schemas.wbem.wsman._1.wsman.AttributableURI;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3._2003._05.soap_envelope.Envelope;
import org.w3._2003._05.soap_envelope.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlsoap.schemas.ws._2004._08.addressing.AttributedURI;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferencePropertiesType;
import org.xmlsoap.schemas.ws._2004._09.mex.Metadata;
import org.xmlsoap.schemas.ws._2004._09.mex.MetadataSection;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.mex.MetadataUtility;
import com.sun.ws.management.server.EnumerationItem;
//import com.sun.ws.management.server.handler.wsman.metadata_Handler;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.transfer.TransferUtility;
import com.sun.ws.management.transport.HttpClient;
import com.sun.ws.management.xml.XmlBinding;

/** This class is responsible for processing Wiseman-annotated code
 *  to populate information on how to contact these handler/endpoints. 
 * 
 * @author Simeon
 *
 */
public class AnnotationProcessor {
	
	private static final Logger LOG = Logger.getLogger(AnnotationProcessor.class.getName());

	/* Define contants helpful in Annotation Processing for metadata.
	 */
	public static final String NS_PREFIX ="wsmeta"; 
	public static final String NS_URI = "http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd";
	// QNAMES used in meta data processing
	public static final QName META_DATA_CATEGORY = new QName(NS_URI,"MetaDataCategory",NS_PREFIX);
	public static final QName META_DATA_DESCRIPTION = new QName(NS_URI,"MetaDataDescription",NS_PREFIX);
	public static final QName META_DATA_TO = new QName(NS_URI,"MetaDataTo",NS_PREFIX);
	public static final QName META_DATA_ENABLED = new QName(NS_URI,"MetaDataEnabled",NS_PREFIX);
	public static final QName META_DATA_RESOURCE_URI = new QName(NS_URI,"MetaDataResourceURI",NS_PREFIX);
	public static final QName RESOURCE_META_DATA_UID = new QName(NS_URI,"ResourceMetaDataUID",NS_PREFIX);
	public static final QName RESOURCE_MISC_INFO = new QName(NS_URI,"ResourceMiscInfo",NS_PREFIX);
	public static final QName ENUMERATION_ACCESS_RECIPE = new QName(NS_URI,"EnumerationAccessRecipe",NS_PREFIX);
	public static final QName ENUMERATION_FILTER_USAGE = new QName(NS_URI,"EnumerationFilterUsage",NS_PREFIX);
	public static final QName[] DESCRIPTIVE_METADATA_ELEMENTS ={META_DATA_CATEGORY,
		META_DATA_DESCRIPTION,META_DATA_TO,META_DATA_ENABLED,META_DATA_RESOURCE_URI,
		RESOURCE_META_DATA_UID,
		ENUMERATION_ACCESS_RECIPE,ENUMERATION_FILTER_USAGE,RESOURCE_MISC_INFO};
	
	public static final String NO_ACTION_NECESSARY= "(NO_ACTION_NECESSARY)";
	
	public static org.w3._2003._05.soap_envelope.ObjectFactory envFactory 
	= new org.w3._2003._05.soap_envelope.ObjectFactory();
	
	//Define the JAXB object factory references for un/marshalling
	private static org.xmlsoap.schemas.ws._2004._09.mex.ObjectFactory metaFactory = 
		new org.xmlsoap.schemas.ws._2004._09.mex.ObjectFactory();
	
	private static net.java.dev.wiseman.schemas.metadata.messagetypes.ObjectFactory 
			metadataContent_fact = new 
	  net.java.dev.wiseman.schemas.metadata.messagetypes.ObjectFactory();
	
	private static XmlBinding binding = null;
	static {
		try {
		  if(binding==null){	
			Management man = new Management();
			binding = man.getXmlBinding();
			//loadServerCredentials
			MetadataUtility.loadServerAccessCredentials(null);
		  }
		} catch (SOAPException e) {
			LOG.severe(e.getMessage());
		}
	}

	
	//Define default namespace context for Metadata processing
	private static NamespaceContext META_DATA_NAMESPACE_CONTEXT = null;

	/** Method takes a defaultAddressingModelAnnotation instance and places
	 * all of the values into a Management instance. 
	 * 
	 * @param defAddMod is the annotation instance.
	 * @return Management instance with all of the properties for a resource using the
	 * 		   defaultAddressingModel.
	 * @throws JAXBException
	 * @throws SOAPException
	 */
	public static Management 
		populateManagementInstance(
			WsManagementDefaultAddressingModelAnnotation defAddMod) 
			throws JAXBException, 
			SOAPException {
		
	//Walk through the values of the annotation to populate Management reference
	Management metaData = new Management();
	
	//exit out if the annotation passed in is invalid
	if(defAddMod==null){
		LOG.log(Level.FINE,"The Annotation passed in is null.");
		return metaData;
	}
		//if address details are present, then populate
	   if(defAddMod.getDefaultAddressDefinition()!=null){
		populateManagementAddressDetails(defAddMod.getDefaultAddressDefinition(), metaData);
	   }
		//Process additional metaData values and add to Mgmt inst.
		if((defAddMod.metaDataCategory()!=null)&&
				(defAddMod.metaDataCategory().trim().length()>0)){
			metaData.addHeaders(
					Management.createReferenceParametersType(
							META_DATA_CATEGORY,
							defAddMod.metaDataCategory()));
		}
		if((defAddMod.metaDataDescription()!=null)&&
				(defAddMod.metaDataDescription().trim().length()>0)){
			metaData.addHeaders(
					Management.createReferenceParametersType(
							META_DATA_DESCRIPTION,
							defAddMod.metaDataDescription()));
		}
		if((defAddMod.resourceMetaDataUID()!=null)&&
				(defAddMod.resourceMetaDataUID().trim().length()>0)){
			metaData.addHeaders(
					Management.createReferenceParametersType(
							RESOURCE_META_DATA_UID,
							defAddMod.resourceMetaDataUID()));
		}
		if((defAddMod.resourceMiscellaneousInformation()!=null)&&
				(defAddMod.resourceMiscellaneousInformation().trim().length()>0)){
			metaData.addHeaders(
					Management.createReferenceParametersType(
					RESOURCE_MISC_INFO,
					defAddMod.resourceMiscellaneousInformation()));
		}
		
	   //if Message typing information present then populate the body.		
	   if((defAddMod.definedOperations()!=null)||
			   (defAddMod.schemaList()!=null)){
		 populateMessageBodyDetails(defAddMod,metaData); 
	   }
		
	  return metaData;
	}

	private static void populateMessageBodyDetails(WsManagementDefaultAddressingModelAnnotation 
			defAddMod, Management metaData) throws JAXBException, SOAPException {
		if(defAddMod!=null){
			MessageDefinitions messTypeDetails = 
				metadataContent_fact.createMessageDefinitions();
			//Locate the schema list if present
			String[] schemaList = defAddMod.schemaList();
			if((schemaList!=null)&&(schemaList.length>0)){
			   SchemasType schemas = metadataContent_fact.createSchemasType();	
			   //Ex. "tl=http://schemas.wiseman.dev.java.net/traffic/1/light.xsd"
			   for(String schemaDef: schemaList){
				if(schemaDef.trim().length()>0){
				  //instantiate jaxb component 
				  SchemaType schema = metadataContent_fact.createSchemaType();
				  //split into relevant components
				  StringTokenizer bag = new StringTokenizer(schemaDef,"=");
				  if(bag.countTokens()==2){
				   schema.setPrefix(bag.nextToken());
				   schema.setValue(bag.nextToken());
				  }else{
					schema.setValue(schemaDef);  
				  }
				  schemas.getSchema().add(schema);
			   }
			   //add list to messageDefinition type
			   if(schemas.getSchema().size()>0){	
			    messTypeDetails.setSchemas(schemas);
			   }
			 }
			}//End of schemaList processing
			
			//Locate the Operations information if present
			WsManagementOperationDefinitionAnnotation[] operationList = 
				defAddMod.definedOperations();
			if((operationList!=null)&&(operationList.length>0)){
			   OperationsType operations = 
				   metadataContent_fact.createOperationsType();	
			   for(WsManagementOperationDefinitionAnnotation annot: operationList){
				OperationNodeType messType = metadataContent_fact.createOperationNodeType();
				if((annot.operationName()!=null)&&(annot.operationName().trim().length()>0)){
					
				 //Ex. MessageType="Create"  
				 //Ex. "tl:TrafficLightTypeMessage=http://schemas.xmlsoap.org/ws/2004/09/transfer/Create";
				 if((annot.operationName()!=null)&&(annot.operationName().trim()!="")){
				   messType.setName(annot.operationName());
				 }
				 //process in/output types
				 String value = "";
				 if(((value=annot.operationInputTypeMap())!=null)&&
						 (value.trim()!="")){
					 InputType input = metadataContent_fact.createInputType();
					 //split into relevant components
					 StringTokenizer bag = new StringTokenizer(
							 value.trim(),
							 "=");
					 if(bag.countTokens()==2){
						 input.setMessage(bag.nextToken());
						 input.setAction(bag.nextToken());
					 }else{
						 input.setMessage("unkwn");
						 input.setAction(value);
					 }
					 messType.setInput(input);
				 }//end of input map definition
				 if(((value=annot.operationOutputTypeMap())!=null)&&
						 (value.trim()!="")){
				   OutputType output = metadataContent_fact.createOutputType();
				   //split into relevant components
				   StringTokenizer bag = new StringTokenizer(
						   value.trim(),
						  "=");
				   if(bag.countTokens()==2){
					  output.setMessage(bag.nextToken());
					  output.setAction(bag.nextToken());
				   }else{
					  output.setMessage("unkwn");
					  output.setAction(value);
				   }
				   messType.setOutput(output);
				 }//end of output map definition
				}//End of if not all values supplied
				if((messType.getName()!=null)&&
						(messType.getName().trim().length()>0)){
				 operations.getOperation().add(messType);
				 messTypeDetails.setOperations(operations);
				}
			   }//end of Anotation processing
			}
			
			
		  if((messTypeDetails.getOperations()!=null)
				  &&(messTypeDetails.getOperations().getOperation().size()>0)
				  &&(messTypeDetails.getSchemas()!=null)
				  &&(messTypeDetails.getSchemas().getSchema().size()>0)){
			//Now convert the JAXB type and insert into Management body
			Document content = Management.newDocument();

			try {
				Management man = new Management();
				binding = man.getXmlBinding();
				binding.marshal(messTypeDetails, content);
				metaData.getBody().addDocument(content);
			} catch (JAXBException e1) {
				e1.printStackTrace();
				LOG.severe(e1.getMessage());
				throw e1;
			} catch (SOAPException e) {
				e.printStackTrace();
				LOG.severe(e.getMessage());
				throw e;
			}
		  }
		}
	}

	/** Method takes a defaultAddressingModelAnnotation instance and places
	 * all of the values into a Management instance. 
	 * 
	 * @param enumSrc is the annotation instance.
	 * @return Management instance with all of the properties for a resource using the
	 * 		   defaultAddressingModel.
	 * @throws JAXBException
	 * @throws SOAPException
	 */
	public static Management 
		populateManagementInstance(
			WsManagementEnumerationAnnotation enumSrc) 
			throws JAXBException, 
			SOAPException {
			Management defAddValue = 
				populateManagementInstance(enumSrc.getDefaultAddressModelDefinition());
			//Add the additional Enumeration values
			if((enumSrc.resourceEnumerationAccessRecipe()!=null)&&
					(enumSrc.resourceEnumerationAccessRecipe().trim().length()>0)){
				defAddValue.addHeaders(
						Management.createReferenceParametersType(
								ENUMERATION_ACCESS_RECIPE,
								enumSrc.resourceEnumerationAccessRecipe()));
			}
			if((enumSrc.resourceFilterUsageDescription()!=null)&&
					(enumSrc.resourceFilterUsageDescription().trim().length()>0)){
				defAddValue.addHeaders(
						Management.createReferenceParametersType(
							ENUMERATION_FILTER_USAGE,
							enumSrc.resourceFilterUsageDescription()));
			}
		return defAddValue;
	}
	
	/**Takes the WsManagementAddressDetailsAnnotation annotation and puts all of the
	 * values into the Management instance passed in.
	 * @param defAddMod
	 * @param metaData
	 * @throws JAXBException
	 * @throws SOAPException
	 */
	private static void populateManagementAddressDetails(
			WsManagementAddressDetailsAnnotation defAddMod, 
			Management metaData) throws JAXBException, SOAPException {
		if(defAddMod!=null){
			  
			//Retrieve the DefaultAddressDetails annotation data
			WsManagementAddressDetailsAnnotation addDetails = 
				defAddMod;
			if(addDetails!=null){
			  //populate the address details node
				//Process referenceParameters entered
				if(addDetails.referenceParametersContents()!=null){
					WsManagementQNamedNodeWithValueAnnotation[] refParams = 
						addDetails.referenceParametersContents();
					//For each reference parameter located...
					for (int i = 0; i < refParams.length; i++) {
						WsManagementQNamedNodeWithValueAnnotation param = refParams[i];
						QName nodeId = new QName(param.namespaceURI(),
								param.localpart(),param.prefix());
					  if((param.nodeValue()!=null)&&(param.nodeValue().trim().length()>0)){	
						ReferenceParametersType refParamType = 
							Management.createReferenceParametersType(nodeId, 
									param.nodeValue());
						metaData.addHeaders(refParamType);
					  }
					}
				}
				//Process referenceProperties entered
				if(addDetails.referencePropertiesContents()!=null){
				   WsManagementQNamedNodeWithValueAnnotation[] refProps = 
					   addDetails.referencePropertiesContents();
				   //For each reference property located...
				   for (int i = 0; i < refProps.length; i++) {
					  WsManagementQNamedNodeWithValueAnnotation param = refProps[i];
					  QName nodeId = new QName(param.namespaceURI(),
							  param.localpart(),param.prefix());
					if((param.nodeValue()!=null)&&(param.nodeValue().trim().length()>0)){							  
					  ReferencePropertiesType refPropType = 
						  Management.createReferencePropertyType(nodeId, 
								  param.nodeValue());
					  metaData.addHeaders(refPropType);
					}
				   }
				}
				//process additional metadata information
				if((addDetails.wsaTo()!=null)&&(addDetails.wsaTo().trim().length()>0)){
					metaData.setTo(addDetails.wsaTo().trim());
				}
				if((addDetails.wsmanResourceURI()!=null)&&
						(addDetails.wsmanResourceURI().trim().length()>0)){
					metaData.setResourceURI(addDetails.wsmanResourceURI().trim());
				}
				//Process the selectorSet values from metadata
				if((addDetails.wsmanSelectorSetContents()!=null)&&
						(addDetails.wsmanSelectorSetContents().length>0)){
					//build the selector set contents
					Set<SelectorType> set = new HashSet<SelectorType>();
					String[] array = addDetails.wsmanSelectorSetContents();
					for (int i = 0; i < array.length; i++) {
						String pair = array[i].trim();
						 pair=pair.substring(0,pair.length());
						 pair=pair.trim();
						StringTokenizer tokens = 
							new StringTokenizer(pair,"=");
						SelectorType st = 
							Management.FACTORY.createSelectorType();
						if((tokens.hasMoreTokens())&&(tokens.countTokens()>1)){
						 st.setName(tokens.nextToken());
						 st.getContent().add(tokens.nextToken());
						 set.add(st);
						}
					}
					if(set.size()>0){
					 metaData.setSelectors(set);
					}
				}
			}
		  }//end of defaultAddressDefinition
	}
	public static Vector<Annotation> 
	  populateAnnotationsFromClass(Class element){
		Vector<Annotation> allAnots = new Vector<Annotation>();
		  //null check. 
		  if(element==null){
			  return allAnots;
		  }
		  //TODO: analyze to see if this is too stringent
		  //make sure than annotated classes are instances of Handler 
		  boolean isHandlerInst = true;
//		  boolean isHandlerInst = false;
//		  Class[] vals = element.getInterfaces();
//		  for (int i = 0; i < vals.length; i++) {
//			  if(vals[i].getCanonicalName().equals(Handler.class.getCanonicalName())){
//				 isHandlerInst = true; 
//			  }
//		  }
		  if(isHandlerInst){
		   //process the class for annotations
			  if(element.isAnnotationPresent(WsManagementDefaultAddressingModelAnnotation.class)){
				  Annotation annotation = 
					  element.getAnnotation(WsManagementDefaultAddressingModelAnnotation.class);
				  WsManagementDefaultAddressingModelAnnotation defAddMod = 
					  (WsManagementDefaultAddressingModelAnnotation)annotation;
				  allAnots.add(defAddMod);
				  
				  //test the Annotated class to see if class fields are also annotated. 
				  //Enables a scalable Annotation model. 
				  Field[] classFields = element.getDeclaredFields();
				  for (int i = 0; i < classFields.length; i++) {
					  Field variable = classFields[i];
					  if(variable.isAnnotationPresent(WsManagementDefaultAddressingModelAnnotation.class)){
						  WsManagementDefaultAddressingModelAnnotation defAddModAnnot = 
							  (WsManagementDefaultAddressingModelAnnotation)variable.getAnnotation(
									  WsManagementDefaultAddressingModelAnnotation.class);
						  allAnots.add(defAddModAnnot);	
					  }
				  }
			  }
			  if(element.isAnnotationPresent(WsManagementEnumerationAnnotation.class)){
				  Annotation annotation = 
					  element.getAnnotation(WsManagementEnumerationAnnotation.class);
				  WsManagementEnumerationAnnotation enumSource = 
					  (WsManagementEnumerationAnnotation)annotation;
				  allAnots.add(enumSource);
				  
				  //test the Annotated class to see if class fields are also annotated. 
				  //Enables a scalable Annotation model. 
				  Field[] classFields = element.getDeclaredFields();
				  for (int i = 0; i < classFields.length; i++) {
					Field variable = classFields[i];
					if(variable.isAnnotationPresent(WsManagementEnumerationAnnotation.class)){
					  WsManagementEnumerationAnnotation enuSrcAnnot = 
						  (WsManagementEnumerationAnnotation)variable.getAnnotation(
								  WsManagementEnumerationAnnotation.class);
					  allAnots.add(enuSrcAnnot);	
					}
				  }
			 }
          }
//		  else{//class is not an instance of Handler.
//        	  LOG.log(Level.FINE,"The class '"+element.getCanonicalName()+
//        			  "' not an instance of Handler."); 
//          }	
		return allAnots;	
	 }
	 
		/**
		 * @param section
		 * @param instance
		 * @throws JAXBException
		 * @throws SOAPException
		 */
		public static Management populateMetadataInformation(MetadataSection section, 
				Management instance) throws JAXBException, SOAPException {
			if(instance==null){
				instance = new Management();
				return instance;
			}
			if(section==null){
				return instance;
			}
			if(binding==null){
			   binding = instance.getXmlBinding();	
			}
			
			//Retrieve the custom content of this MetadataSection.
			Object customDialectContent = section.getAny(); 				
			
			//Translate the Metadata node to a Management instance.
			Envelope env = envFactory.createEnvelope();
			JAXBElement<Envelope> envelope = 
				(JAXBElement<Envelope>) customDialectContent;
			//bail out if envelope not uncovered.
			if(envelope==null){
				return instance;
			}
			try{
			env = envelope.getValue();
			}
			catch(ClassCastException cce){
			   LOG.severe("Unable to locate metadata content:"+cce.getMessage());	
				return instance;
			}
			
			   List<Object> headerList = env.getHeader().getAny();
			   for (Iterator iter = headerList.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();
				  
				  //if header is instance of ElementNSImpl
				  if(element instanceof ElementNSImpl){
				   ElementNSImpl e = (ElementNSImpl) element;
				    QName node = AnnotationProcessor.populateNode(e);
				    instance.addHeaders(
						Management.createReferenceParametersType(
							node,
							e.getTextContent()));
				  }else if(element instanceof JAXBElement){
					  JAXBElement jel = (JAXBElement) element;
					  if(jel.getDeclaredType().equals(AttributableURI.class)){
						AttributableURI e = (AttributableURI) jel.getValue();
						if(Management.RESOURCE_URI.equals(jel.getName())){
							instance.setResourceURI(e.getValue());
						}
					  }
					  else if(jel.getDeclaredType().equals(AttributedURI.class)){
						  AttributedURI atUri = (AttributedURI) jel.getValue();
						  if(Addressing.TO.equals(jel.getName())){
							  instance.setTo(atUri.getValue());
						  }else if(Management.RESOURCE_URI.equals(jel.getName())){
							  instance.setResourceURI(atUri.getValue());
						  }
					  }
					  else if(jel.getDeclaredType().equals(SelectorSetType.class)){
						  SelectorSetType sel = (SelectorSetType) jel.getValue();
						  HashSet selSet = new HashSet<SelectorType>(sel.getSelector());
						  instance.setSelectors(selSet);
					  }
				  }else{
				  }
			   }//End of processing for SoapEnv Headers
			   
			   //Process for Envelope Body
			   if((env.getBody().getAny()!=null)
					   &&(env.getBody().getAny().size()>0)){
 	 			 Document bodyDoc = Management.newDocument();
 	 			 try{
				  binding.marshal(env.getBody().getAny().get(0),
						bodyDoc);
				  instance.getBody().addDocument(bodyDoc);
				 }catch (JAXBException e) {
					e.printStackTrace();
				 }catch (SOAPException soex){
					soex.printStackTrace();
				 }
			   }
			  return instance; 
		}

	public static QName populateNode(ElementNSImpl e) {
		return new QName(e.getNamespaceURI(),e.getLocalName(),e.getPrefix());
	}
	
	/**
	 * @param metaElement
	 * @param element
	 * @throws JAXBException
	 * @throws SOAPException
	 */
	public static Metadata populateMetaDataElement(Metadata metaElement, Management element) 
				throws JAXBException, SOAPException {
		if((metaElement==null)||(element ==null)){
			return metaElement;
		}
		//Check to see if the Management element is empty and if so then bail out.
		if(element.getHeaders().length==0){
			return metaElement;
		}
		
		//Create enclosing MetaDataSection element
		MetadataSection metaSection = metaFactory.createMetadataSection();
		 metaElement.getMetadataSection().add(metaSection);
		 metaSection.setDialect(AnnotationProcessor.NS_URI); 
		 metaSection.setIdentifier(AnnotationProcessor.NS_URI);
		 
		 //Now populate the MetaData specific element/any node.
		 if((element.getBody()!=null)&&
				 (element.getBody().getFirstChild()!=null)){
		   Object customDialectContent = element.getBody().getFirstChild();
		   SOAPEnvelope popEnv = element.getEnvelope();
		   SOAPBody envContents = popEnv.getBody();
		   if(envContents==null){envContents=popEnv.addBody();}
		   
			try{ 
				Node bodyDoc = (Node) customDialectContent;
				envContents.addDocument(bodyDoc.getOwnerDocument());
			}catch (Exception e) {
				// TODO: handle exception
			   e.printStackTrace();
			}
		   metaSection.setAny(popEnv);
		 }else{
		 metaSection.setAny(element.getEnvelope());
		 }
		    
		return metaElement;    
	}

	 public static String xmlToString(Node node) {
			try {
				Source source = new DOMSource(node);
				StringWriter stringWriter = new StringWriter();
				Result result = new StreamResult(stringWriter);
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer transformer = factory.newTransformer();
				transformer.transform(source, result);
				return stringWriter.getBuffer().toString();
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			return null;
	}

	public static List<Management> extractMetaDataFromEnumerationMessage(
			final List<EnumerationItem> items) throws SOAPException, JAXBException{
		//return reference
		List<Management> metadataInstances = new ArrayList<Management>();
		//process for null
		if(items == null){
		   return metadataInstances;	
		}
		
		//Walk through List<EnumerationItem> and translate
		for(EnumerationItem located: items){
			//retrieved item
			Object item = located.getItem();
			
			//Find the mangement envelope or the mess instances
			Envelope env = envFactory.createEnvelope();
			
		     JAXBElement<Envelope> envelope = (JAXBElement<Envelope>) item;
			 env = envelope.getValue();
			
			if(env!=null){
				Management src = new Management();
				Header header = env.getHeader();
				ReferenceParametersType ref = 
					Management.createReferenceParametersType(
							Transfer.ACTION, 
							"(Insert-Valid-Action)");
				for(Object hed: header.getAny()){
					ref.getAny().add(hed);
				}
				src.addHeaders(ref);
				//Now process for body elements
				if(env.getBody().getAny().size()>0){
  	 			 Document bodyDoc = Management.newDocument();
	 			 try{
				   binding.marshal(env.getBody().getAny().get(0),
					bodyDoc);
				  src.getBody().addDocument(bodyDoc);
				 }catch (JAXBException e) {
					e.printStackTrace();
				 }catch (SOAPException soex){
						soex.printStackTrace();
				 }
				}
				
				metadataInstances.add(src);
			}
			
		}//end of convert to Management instances
	 return metadataInstances;
	}

	/** Method automates the task of locating the MetaData information for a single 
	 * annotated resource. The metaUidForAnnotatedResource is the UID 
	 * field(wsmeta:ResourceMetaDataUID) for a known Resource intance.  This may 
	 * be known or discovered before submission.  The returned MetaData/Management 
	 * instance has all of the fields for locating/specifying a handler prepopulated.
	 *
	 * Steps taken to extract this data:
	 * i)Contact the Wiseman server passed in via identify to wisemanServer parameter.
	 * ii)Parse the Identify response to locate the metadata handler details
	 * iii)Submit optimized enumerate request filtering on the wsmeta:ResourceMetaDataUID
	 * 		field.
	 * iv)Convert the returned values back to Management instance
	 * v) Return that prepopulated Management message.   
	 * 
	 * @param metaUidForAnnotatedResource  Ex. http://wiseman.dev.java.net/EventSource/eventcreator/uid-20000747652
	 * @param wisemanServer Ex. http://localhost:8080/wsman/
	 * @param emptyPayload Ex. true
	 * @return Management object
	 * @throws SOAPException 
	 * @throws IOException 
	 * @throws DatatypeConfigurationException 
	 * @throws JAXBException 
	 */
	public static Management findAnnotatedResourceByUID(String metaUidForAnnotatedResource,
			String wisemanServer, boolean emptyPayload, QName... headersToPrune  ) throws SOAPException, 
			JAXBException, 
			DatatypeConfigurationException, IOException 
		{
		Management locatedResource= null;
		
//		//build the XPath expression for the filtered enumeration
//		  String filter = "env:Envelope/env:Header/wsmeta:ResourceMetaDataUID/text()='"+
//		  metaUidForAnnotatedResource+"'";
//		  
//		//construct Optimized enumerate request 
//		  EnumerationMessageValues enuSettings = 
//			  EnumerationMessageValues.newInstance(); 
//		  enuSettings.setFilter(filter);
//	      enuSettings.setRequestForOptimizedEnumeration(true);
//	
//		    Enumeration optimizeEnumRequestFilteredEventSources = 
//		  	  EnumerationUtility.buildMessage(null, enuSettings);
//		    //Now build the request from the Enumeration component
//		    Management filWsmanRequest = ManagementUtility.buildMessage(
//		  		optimizeEnumRequestFilteredEventSources,
//		  		null);
//		    if(wisemanServer ==null){
//		    	throw new IllegalArgumentException("Unable to locate Wiseman server"+
//		    			" destination.");
//		    }
//		    filWsmanRequest.setTo(wisemanServer);
//		    filWsmanRequest.setResourceURI(metadata_Handler.wsaResourceURI);
//		    
//		    //Send the request.
//		    Addressing filterMetaResponse = HttpClient.sendRequest(filWsmanRequest);
//	        if (filterMetaResponse.getBody().hasFault()) {
//	          String msg = "Unable to find annotated resource using the ";
//	          msg+="the XPath filter '"+filter+"' because of :";
//	          msg+=filterMetaResponse.getBody().getFault().getFaultString();
//	          Iterator st = 
//	        	  filterMetaResponse.getBody().getFault().getFaultReasonTexts();
//	          while (st.hasNext()) {
//				String element = (String) st.next();
//				msg+="\n"+element;
//			  }
//	          throw new RuntimeException(msg);
//	        }
//
//		       //Extract the response 
//			   List<EnumerationItem> locatedMetadata = 
//					EnumerationUtility.extractEnumeratedValues(
//							new Management(filterMetaResponse));
//			
//			   //translate EnumerationItem list into List<Management>
//			  List<Management> resourcesLocated = AnnotationProcessor.
//			  		extractMetaDataFromEnumerationMessage(locatedMetadata);
//			  
//			  if((resourcesLocated!=null)&&(resourcesLocated.size()>0)){
//				 if(resourcesLocated.size()==1){
//					 locatedResource = resourcesLocated.get(0);	 
//				 }else{
//					String msg = "The Xpath expression '"+metaUidForAnnotatedResource+"' passed in ";
//					msg+="did not map to a unique resource."+resourcesLocated.size()+
//					" resource(s) were located.";
//					throw new RuntimeException(msg);
//				 }
//			  }
			
//		String filter = "env:Envelope/env:Header/wsmeta:ResourceMetaDataUID/text()='"+
////		MetadataTest.enuMetaDataUID+"'";
//		metaUidForAnnotatedResource.trim()+"'";
//System.out.println("@@@@ Filter:"+filter);		
//		EnumerationMessageValues enuValues = EnumerationMessageValues.newInstance();
//		 enuValues.setRequestForOptimizedEnumeration(true);
//		 enuValues.setFilter(filter);
//		Enumeration enMesg = EnumerationUtility.buildMessage(null, enuValues); 
//		Management filteredMetaDataReq = ManagementUtility.buildMessage(enMesg,null);
//		  filteredMetaDataReq.setTo(wisemanServer);
//		  filteredMetaDataReq.setResourceURI(metadata_Handler.wsaResourceURI);
//		//Send the Enumeration request
//	     final Addressing response = HttpClient.sendRequest(filteredMetaDataReq);
//	     if((response!=null)&&(response.getBody()!=null)&&
//	    	(!response.getBody().hasFault())){
//	     
//		    //Parse response for recognizable Management instances 
//		     //Translate the OptimizedEnumeration results to List<EnumerationItem> 
//		     Management mResp = new Management(response);
//		     
//		   	//EnumerationResourceState state = EnumerationUtility.extractResourceState(mResp);
//		     //Extract the response
//		   	  List<EnumerationItem> located = EnumerationUtility.extractEnumeratedValues(mResp);
//	//	   	  assertEquals("EventSources count not correct.",1, state.size());
//		   	  if(located.size()>0){
//			    //translate EnumerationItem list into List<Management>
//			    List<Management> resourcesLocated = AnnotationProcessor.
//			  		extractMetaDataFromEnumerationMessage(located);
//			    
//			    if(resourcesLocated.size()>0){
//			       if(resourcesLocated.size()==1){
//					 locatedResource = resourcesLocated.get(0);	 
//				   }else{
//						String msg = "The Xpath expression '"+metaUidForAnnotatedResource+"' passed in ";
//						msg+="did not map to a unique resource."+resourcesLocated.size()+
//						" resource(s) were located.";
//						throw new RuntimeException(msg);
//				   }
//			    }
//		   	  }
//	     }//END of if has body	
		
		//#####################################
//		if(metadataIsStaticRef==null){
//			String value = WSManServlet.getMetadataProperty("metadata.content.dynamic");
////System.out.println("@@@ metadata.content.dynamic:"+value+":");			
//			if((value!=null)&&(!value.trim().equals(""))){
//			  try{
//				metadataContentIsStatic = !Boolean.valueOf(value); 
//				metadataIsStaticRef = new Object();
//			  }catch(Exception ex){
//				 //eat exception. 
//			  }
//			}
//		}
//		if(metadataContentIsStatic){
//			if(cachedMetadataList!=null){
//				for(Management meta: cachedMetadataList){
//	 	        	SOAPElement uid = ManagementUtility.locateHeader(meta.getHeaders(), 
//		        			AnnotationProcessor.RESOURCE_META_DATA_UID);
//	 	        	if((uid!=null)&&(!uid.getTextContent().trim().equals(""))){
//	 	        	 if(uid.getTextContent().trim().equals(metaUidForAnnotatedResource)){
//		        		locatedResource = meta;
//		        	 }
//	 	        	}
//				}
//			}else{
//			  try{	
//				//Now make calls to initially populate the list
//		         locatedResource = 
//		        	 getExposedMetadataForInstance(metaUidForAnnotatedResource, 
//		        		wisemanServer, locatedResource);
//			  }catch(IOException iex){
//				  String msg = "An attempt to access the Metadata repository at '";
//				  msg+=wisemanServer+"' failed.\n Check that the server details are correct and" +
//				  " examine the following failure details:\n";
//				  msg+=iex.getMessage();
//				  if(iex.getCause()!=null){
//					iex.getCause().getMessage();  
//				  }
//				 throw new RuntimeException(msg);
//			  }
//			}
//		}else{
		  try{	
            locatedResource = 
        	 getExposedMetadataForInstance(metaUidForAnnotatedResource, 
        		wisemanServer, locatedResource);
		  }catch(IOException iex){
			  String msg = "An attempt to access the Metadata repository at '";
			  msg+=wisemanServer+"' failed.\n Check that the server details are correct and" +
			  " examine the following failure details:\n";
			  msg+=iex.getMessage();
			  if(iex.getCause()!=null){
				iex.getCause().getMessage();  
			  }
			 throw new RuntimeException(msg);
		  }
//		}
		
		//####################################
		if(locatedResource==null){
			String msg = "Metadata with the ResourceID '"+metaUidForAnnotatedResource+
				"' could not be found at the server '"+wisemanServer+"'.";
			throw new RuntimeException(msg);
		}else{//Successfully located the Metadata for the service

			//figure out whether to remove additional elements
			removeUnwantedMetadataContent(emptyPayload, locatedResource, headersToPrune);
		}
		return locatedResource;
	}

	/**
	 * @param emptyPayload
	 * @param locatedResource
	 * @param headersToPrune
	 * @throws SOAPException
	 */
	private static Management removeUnwantedMetadataContent(boolean emptyPayload, 
			Management locatedResource, 
			QName... headersToPrune) throws SOAPException {
		
		if(locatedResource==null){
			String msg="The Management resource instance passed in cannot be NULL.";
			throw new IllegalArgumentException(msg);
		}
		
		if(emptyPayload){
			locatedResource.getBody().removeContents();
		}
		//mechanism to remove arbitrary Header elements.
		if((headersToPrune!=null)&&(headersToPrune.length>0)){
			//Existing headers
		  SOAPElement[] headerList = locatedResource.getHeaders();
		   //The new header list
		  ArrayList<SOAPElement> retainList = new ArrayList<SOAPElement>();
		   //populate prune list.
		  ArrayList<QName> pruneList = new ArrayList<QName>();
		  for(QName tmp:headersToPrune){
			  pruneList.add(tmp);
		  }
		  
		  //iterate through the header and find elements to retain.
		  for (int i = 0; i < headerList.length; i++) {
			  SOAPElement header = headerList[i];
			if(!pruneList.contains(header.getElementQName())){
			  //add if it's not already been added.	
			  if(!retainList.contains(header)){
			    retainList.add(header);
			  }
			}
		  }
		  //Reassign the header list if necessary
		 if(!retainList.isEmpty()){
		   //purge old	 
		   locatedResource.getHeader().removeContents();
		   //replace with the retain content
		   for (Iterator iter = retainList.iterator(); iter.hasNext();) {
			 SOAPElement element = (SOAPElement) iter.next();
			 locatedResource.getHeader().addChildElement(element);
		   }
		 }
		}
		return locatedResource;
	}
	
	/** Method automates the task of locating the MetaData information for a single 
	 * annotated resource. The metaUidForAnnotatedResource is the UID 
	 * field(wsmeta:ResourceMetaDataUID) for a known Resource intance.  This may 
	 * be known or discovered before submission.  The returned MetaData/Management 
	 * instance has all of the fields for locating/specifying a handler prepopulated.
	 *
	 * Steps taken to extract this data:
	 * i)Contact the Wiseman server passed in via identify to wisemanServer parameter.
	 * ii)Parse the Identify response to locate the metadata handler details
	 * iii)Submit optimized enumerate request filtering on the wsmeta:ResourceMetaDataUID
	 * 		field.
	 * iv)Convert the returned values back to Management instance
	 * v) Return that prepopulated Management message.   
	 * 
	 * @param metaUidForAnnotatedResource  Ex. http://wiseman.dev.java.net/EventSource/eventcreator/uid-20000747652
	 * @param wisemanServer Ex. http://localhost:8080/wsman/
	 * @return management object
	 * @throws SOAPException
	 * @throws JAXBException
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 */
	public static Management findAnnotatedResourceByUID(
			String metaUidForAnnotatedResource,
			String wisemanServer) throws SOAPException, JAXBException, 
			DatatypeConfigurationException, IOException 
	{
	  QName[] headersToTrim = null;
	  return findAnnotatedResourceByUID(metaUidForAnnotatedResource, 
			  wisemanServer, true, headersToTrim);	
	}


	/**
	 * @param metaUidForAnnotatedResource
	 * @param wisemanServer
	 * @param locatedResource
	 * @return management object
	 * @throws DatatypeConfigurationException 
	 * @throws JAXBException 
	 * @throws SOAPException 
	 * @throws IOException 
	 */
	private static Management getExposedMetadataForInstance(
			String metaUidForAnnotatedResource, String wisemanServer, 
			Management locatedResource) throws SOAPException, JAXBException, 
			DatatypeConfigurationException, IOException  {
		Management m = null; 
        	m =TransferUtility.createMessage(wisemanServer, "",
        		Transfer.GET_ACTION_URI, null, null, 30000, null);
        //Parse the getResponse for the MetaData
        final Addressing response = HttpClient.sendRequest(m);
	     if((response!=null)&&(response.getBody()!=null)){
	       if(!response.getBody().hasFault()){
	    	 Management mResp = new Management(response);
	    	 Management[] metaDataList = 
	    		 MetadataUtility.extractEmbeddedMetaDataElements(mResp);
	    	 if((metaDataList!=null)&&(metaDataList.length>0)){
	    		 int located=0;
	    		 for(Management meta: metaDataList){
	 	        	SOAPElement uid = ManagementUtility.locateHeader(meta.getHeaders(), 
		        			AnnotationProcessor.RESOURCE_META_DATA_UID);
	 	        	if((uid!=null)&&(!uid.getTextContent().trim().equals(""))){
	 	        	 if(uid.getTextContent().trim().equals(metaUidForAnnotatedResource)){
		        		locatedResource = meta;
		        		located++;
		        	 }
	 	        	}
	    		 }
				 if(located>0){
			       if(located>1){
						String msg = "The MetadataUID '"+metaUidForAnnotatedResource+"' passed in ";
						msg+="did not map to a unique resource."+located+
						" resource(s) were located.";
						throw new RuntimeException(msg);
				   }
			    }	    	 
	    	 }
	       }else{//response has fault in it
	    	  throw new SOAPFaultException(response.getBody().getFault());    
	       }
	     }
		return locatedResource;
	}
	
	/** 
	 * 
	 * @author Simeon
	 *
	 */
	public static class MetadataNamepaceContext implements NamespaceContext{

		/* Define for prefix mapping for XPath processing.
		 * (non-Javadoc)
		 * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String)
		 */
		public String getNamespaceURI(String prefix) {
		   if ( prefix.equals(NS_PREFIX)) {
		      return NS_URI;
		   } 
		   else if(prefix.equals(SOAP.NS_PREFIX)){
			   return SOAP.NS_URI;
		   }
		   else if(prefix.equals(Management.NS_PREFIX)){
			   return Management.NS_URI;
		   }
		   return null;
		}

		/* Define the namespace mapping for XPath processing.
		 * (non-Javadoc)
		 * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String)
		 */
		public String getPrefix(String namespaceURI) {
		   if ( namespaceURI.equals( 
				   NS_URI)) {
		      return NS_PREFIX;
		   } 
		   else if(namespaceURI.equals(SOAP.NS_URI)){
			   return SOAP.NS_PREFIX;
		   }
		   else if(namespaceURI.equals(Management.NS_URI)){
			   return Management.NS_PREFIX;
		   }
		 return null;
		}

		public Iterator getPrefixes(String namespaceURI) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	/** Returns a NameSpaceContext for XPath processing that knows how to 
	 *  parse metadata env/wsmeta prefixed nodes.
	 * 
	 * @return NamespaceContext as described above.
	 */
	public static NamespaceContext getMetaDataNamespaceContext(){
		if(META_DATA_NAMESPACE_CONTEXT==null){
			META_DATA_NAMESPACE_CONTEXT = new MetadataNamepaceContext();
		}
		return META_DATA_NAMESPACE_CONTEXT;
	}

	/**Remove all metadata-ONLY elements.
	 * 
	 * @param existing
	 * @param removeMetadataBody TODO
	 * @return Management object
	 * @throws SOAPException
	 */
	public static Management stripMetadataContent(Management existing, 
			boolean removeMetadataBody,QName... headersToPrune) throws SOAPException{
		if((existing!=null)&&(existing.getHeaders()!=null)){
			 for(SOAPElement header: existing.getHeaders()){
			  if(AnnotationProcessor.isDescriptiveMetadataElement(
					  header.getElementQName())){
				 existing.getHeader().removeChild(header); 
			  }
		     }//End of iteratation through header list
			 if(removeMetadataBody){
			  if((existing.getBody()!=null)&&
				(existing.getBody().getFirstChild()!=null)){
				existing.getBody().removeContents();  
			   //TODO: does not work. Rework to remove from the Management instance passed in.	  
			  }
			 }
			
		}
		existing = removeUnwantedMetadataContent(removeMetadataBody, 
				existing, headersToPrune);
		return existing;
	}
	
		/**
		 * 
		 * @param header
		 * @param element
		 * @return Node object
		 */
		private static Node containsHeader(SOAPHeader header, SOAPElement element) {
			Node located = null;
			  NodeList chNodes = header.getChildNodes();
			  QName elementNode = element.getElementQName();
			  for (int i = 0; i < header.getChildNodes().getLength(); i++) {
				 Node elem = chNodes.item(i);
				 if((elem.getLocalName().equals(elementNode.getLocalPart()))&&
					(elem.getNamespaceURI().equals(elementNode.getNamespaceURI()))){
					located = elem; 
				 }
			  }
			return located;
		}
		
		
	/**Returns boolean evaluation of whether this QNAME is part of the additional/
	 * descriptive MetaData QNames/Nodes added to a Managment instance.
	 * Addressing.TO or Management.ResourceURI are example of non-descriptive
	 * and core elements.
	 * 
	 * @param elementQName
	 * @return boolean indicating if this is a DescriptiveMetadataElement
	 */
	public static boolean isDescriptiveMetadataElement(QName elementQName) {
		boolean isDescriptive = false;
		for(QName node : DESCRIPTIVE_METADATA_ELEMENTS ){
			if(node.getLocalPart().equals(elementQName.getLocalPart())){
				isDescriptive = true;
			}
		}
		return isDescriptive;
	}
}
