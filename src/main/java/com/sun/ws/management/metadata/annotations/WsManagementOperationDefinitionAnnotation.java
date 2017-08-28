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
 **$Log: WsManagementOperationDefinitionAnnotation.java,v $
 **Revision 1.3  2007/05/30 13:57:31  nbeers
 **Add HP copyright header
 **
 **
 *
 * $Id: WsManagementOperationDefinitionAnnotation.java,v 1.3 2007/05/30 13:57:31 nbeers Exp $
 */
package com.sun.ws.management.metadata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/** This annotation provides a mechanism of defining a
 *  SOAP Operation node analagous to request-response-operation
 *  for wsdl:operationType.
 *
 * Ex.
 * <code><pre>
 * <operation name="Create">
 *    &lt;input message="tns:TrafficLightTypeMessage" wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/Create"/&gt;
 *    &lt;output message="tns:CreateResponseMessage" wsa:Action="http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse"/&gt;
 * </operation>
 * </pre></code>
 */
@Retention(RUNTIME)
@Target(value=ANNOTATION_TYPE)
public @interface WsManagementOperationDefinitionAnnotation {

	/** The value of the operationName is meant to describe the
	 * following input/output operation model at a high level.
	 * Both input/output messages below are used to define one
	 * 'Create' operation.
	 *
	 * Ex. operationName="Create"
	 *     operationInputTypeMap="tns:TrafficLightTypeMessage=http://schemas.xmlsoap.org/ws/2004/09/transfer/Create"
	 *     operationOutputTypeMap="tns:CreateResponseMessage=http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse"
	 */
	public String operationName();

	/** The operationType mapping is used to link a schema defined
	 * type to a defined SoapAction.  In other words, the
	 * 'Transfer.CREATE' soap action will be using the 'TrafficLightTypeMessage'
	 * defined in the light.xsd schema file using the 'tns' prefix.
	 *
	 * Ex. operationInputTypeMap="tns:TrafficLightTypeMessage=http://schemas.xmlsoap.org/ws/2004/09/transfer/Create"
	 */

	public String operationInputTypeMap();
	/** The operationType mapping is used to link a schema defined
	 * type to a defined SoapAction.  In other words, the
	 * 'Transfer.CREATE' soap action will be using the 'TrafficLightTypeMessage'
	 * defined in the light.xsd schema file using the 'tns' prefix.
	 *
	 * Ex. operationOutputTypeMap="tns:CreateResponseMessage=http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse"
	 */
	public String operationOutputTypeMap();

}