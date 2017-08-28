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
 **$Log: WsManagementAddressDetailsAnnotation.java,v $
 **Revision 1.3  2007/05/30 20:30:25  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: WsManagementAddressDetailsAnnotation.java,v 1.3 2007/05/30 20:30:25 nbeers Exp $
 */
package com.sun.ws.management.metadata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

@Retention(RUNTIME)
@Target(value=ANNOTATION_TYPE)
public @interface WsManagementAddressDetailsAnnotation {

    //public String wsTo() default "(RESOURCE NOT DEFINED)";
	//By not defining default values, these fields are required.
	/** This is considered the transport address of the service, on which many listeners or 
	 *  resource/enumeration servers may concurrently exist.
	 *   
	 * <b>wsa:To (required):</b> the transport address of the service.
	 * 
	 * Ex: &lt;wsa:To&gt;http://123.99.222.36/wsman&lt;/wsa:To&gt;
	 */
	public String wsaTo();

	/** This is considered the differentiating URL component for one/many listeners or 
	 *  resource/enumeration server components.
	 *   
	 * <b>wsman:ResourceURI (required):</b> the URI of the resource class representation or 
	 * instance representation.
	 * 
	 * Ex <wsman:ResourceURI>http://acme.org/hardware/2005/02/storage/physDisk</wsman:ResourceURI>
	 */
	public String wsmanResourceURI();

	/** Use selectorSet values to select specific instances if more than one resource exists on 
	 *  the specificied WsaTo and WsManResource istance specified by the two previous parameters.
	 *   
	 * <b>wsman:SelectorSet (optional):</b> Identifies or "selects" the resource instance to be 
	 * accessed if more than once instance of a resource class exists.
	 * <code><pre>
	 * &lt;wsman:SelectorSet&gt; 
	 *   &lt;wsman:Selector Name="FirstName"&gt;James&lt;/wsman:Selector&gt; 
	 *   &lt;wsman:Selector Name="LastName"&gt;Gosling&lt;/wsman:Selector&gt; 
	 * &lt;/wsman:SelectorSet&gt;
	 * </pre></code>
	 * 
	 * Ex. wsmanSelectorSetContents={"FirstName=James","LastName=Gosling"}
	 */
	public String[] wsmanSelectorSetContents() default "(\"\")";
	
	//TODO: figure out how to expose EPRs embedded inside the SelectorSet elements. This is
	//      a rarely used part of the Default Addressing Model.  This annotation cannot 
	//      reference itself otherwise it would have been added right now.  Options are to 
	//      extract the EPR specific content to it's own type and then create a new type that
	//      can then return an array of those EPR types. However this approach will leave the
	//      EPR annotation that will have no real meta data value by itself. :-/ ??
	//	public WsManagementAddressDetailsAnnotation getEmbeddedEPRContent(); 
	
	/** Retrieve optional ReferenceParameter components that my have been passed in as simple
	 *  name=value pairs.
	 *  
	 *  Ex. referenceParametersContents={
	 *  		&#64;WsManagementQNamedNodeWithValueAnnotation(
	 *			localpart="", 
	 *			namespaceURI="", 
	 *			nodeValue="", 
	 *			prefix="") 
	 *  };	 
	 */
	public WsManagementQNamedNodeWithValueAnnotation[] referenceParametersContents() default { 
												@WsManagementQNamedNodeWithValueAnnotation(
														localpart="", 
														namespaceURI="", 
														nodeValue="", 
														prefix="") };
	
	/** Retrieve optional ReferenceProperty components that my have been passed in as simple
	 *  name=value pairs.
	 *  
	 *  Ex. referencePropertiesContents={
	 *  		&#64;WsManagementQNamedNodeWithValueAnnotation(
	 *			localpart="", 
	 *			namespaceURI="", 
	 *			nodeValue="", 
	 *			prefix="") 
	 *  };
	 */
	public WsManagementQNamedNodeWithValueAnnotation[] referencePropertiesContents() default { 
		@WsManagementQNamedNodeWithValueAnnotation(
				localpart="", 
				namespaceURI="", 
				nodeValue="", 
				prefix="") };
	
//	/** Retrieve optional ReferenceProperty components that my have been passed in as simple
//	 *  name=value pairs.
//	 *  
//	 *  Ex. referencePropertiesContents={"name1=val1","name2=val2"}
//	 */
//	public WsManagementQNamedNodeWithValueAnnotation[] customReferencePropertiesContents() default { 
//		@WsManagementQNamedNodeWithValueAnnotation(
//				localpart="", 
//				namespaceURI="", 
//				nodeValue="", 
//				prefix="") };	
}
