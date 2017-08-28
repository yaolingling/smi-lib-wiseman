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
 **$Log: WsManagementQNamedNodeWithValueAnnotation.java,v $
 **Revision 1.3  2007/05/30 20:30:25  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: WsManagementQNamedNodeWithValueAnnotation.java,v 1.3 2007/05/30 20:30:25 nbeers Exp $
 */
package com.sun.ws.management.metadata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/** This annotation provides a mechanism of defining a cutom
 *  QNamed node with a simple text value.
 * 
 * Ex. &lt;id:SpecVersion&gt;1.0.0a&lt;/id:SpecVersion&gt;
 */
@Retention(RUNTIME)
@Target(value=ANNOTATION_TYPE)
public @interface WsManagementQNamedNodeWithValueAnnotation {
	
	/** The value of the namespace uri for the QName of the node
	 * 
	 * Ex: namespaceURI="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"
	 */
	public String namespaceURI() default "";

	/** The value of the localpart for the QName of the node
	 * 
	 * Ex: localpart="ResourceURI"
	 */
	public String localpart() default "";

	/** The value of the prefix for the QName of the node
	 * 
	 * Ex: prefix="wsman"
	 */
	public String prefix() default "";

	/** The value to be inserted into the node
	 * 
	 * Ex: nodeValue="http://acme.org/hardware/2005/02/storage/physDisk"
	 */
	public String nodeValue() default "";
	
}