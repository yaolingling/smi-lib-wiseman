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
 **$Log: WsManagementEnumerationAnnotation.java,v $
 **Revision 1.3  2007/05/30 13:57:31  nbeers
 **Add HP copyright header
 **
 **
 *
 * $Id: WsManagementEnumerationAnnotation.java,v 1.3 2007/05/30 13:57:31 nbeers Exp $
 */
package com.sun.ws.management.metadata.annotations;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** This annotation helps implementers describe useful metadata about
 *  their WS-Management Enumeration implementations.
 *  (Insert Example)
 *
 */
@Retention(RUNTIME)
public @interface WsManagementEnumerationAnnotation {

	/** This defines the Addressing details for this specific EndPoint.
	 *
	 *  Ex.
	 *  &#64;WsManagementDefaultAddressingModelAnnotation(
	 *		getDefaultAddressDefinition =
	 *			&#64;WsManagementAddressDetailsAnnotation(
	 *					referenceParametersContents={""},
	 *					referencePropertiesContents={""},
	 *					wsaTo="http://localhost:8080/wsman/",
	 *					wsmanResourceURI="wsman:auth/user",
	 *					wsmanSelectorSetContents={"firstname=Get",
	 *							"lastname=Guy"}
	 *			),
	 *			metaDataCategory = "PERSON_RESOURCES",
	 *			metaDataDescription = "This resource exposes people information " +
	 *					"stored in an LDAP repository.",
	 *			resourceMetaDataUniqueName =
	 *				"http://some.company.xyz/ldap/repository/2006/uid_00000000007"
	 *			resourceMiscellaneousInformation =
	 *				"This default addressing instance is miscellaneous."
	 *	)
	 */
	public WsManagementDefaultAddressingModelAnnotation getDefaultAddressModelDefinition();

	/** This is an optional field that can be used to describe how to access/query information
	 *  from this enumeration.  A large enumeration can have many fields with duplicate information,
	 *  like many Employees with the same last name. The Enumeration developers can suggest which
	 *  fields or combinations of the fields are searchable and would provide the rest results.
	 *
	 * Ex. resourceEnumerationAccessRecipe="Only FName, LName and Group fields work when " +
	 *     "filtering enumerations. Use Group field data as often as possible for most efficient "+
	 *     " enumeration processing";
	 */
	public String resourceEnumerationAccessRecipe() default "(PROVIDE RECIPE TO ACCESS RESOURCES)";

	/**This is an optional flag that my be used by consumers to help describe metadata
	 * information in a succinct human readable fashion.
	 *
	 * Ex. metaDataDescription="This resource exposes create/remove/update/delete functionality
	 *     for employees in an LDAP repository"
	 */
	public String resourceFilterUsageDescription() default "(INSERT FILTER DOCUMENTATION HERE)";

}

