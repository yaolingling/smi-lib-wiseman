/*
 * Copyright 2005 Sun Microsystems, Inc.
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
 **$Log: not supported by cvs2svn $
 **
 * $Id: UnsupportedFeatureFault.java,v 1.4 2007-05-30 20:31:05 nbeers Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class UnsupportedFeatureFault extends SenderFault {

    public static final QName UNSUPPORTED_FEATURE =
            new QName(Management.NS_URI, "UnsupportedFeature", Management.NS_PREFIX);
    public static final String UNSUPPORTED_FEATURE_REASON =
            "The specified feature is not supported.";

    public static enum Detail {
        AUTHORIZATION_MODE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/AuthorizationMode"),
        ADDRESSING_MODE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/AddressingMode"),
        ACK("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/Ack"),
        ENUMERATION_MODE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/EnumerationMode"),
        OPERATION_TIMEOUT("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/OperationTimeout"),
        LOCALE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/Locale"),
        EXPIRATION_TIME("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/ExpirationTime"),
        FRAGMENT_LEVEL_ACCESS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/FragmentLevelAccess"),
        DELIVERY_RETRIES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/DeliveryRetries"),
        HEARTBEATS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/Heartbeats"),
        BOOKMARKS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/Bookmarks"),
        MAX_ELEMENTS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxElements"),
        MAX_TIME("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxTime"),
        MAX_ENVELOPE_SIZE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxEnvelopeSize"),
        MAX_ENVELOPE_POLICY("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MaxEnvelopePolicy"),
        FILTERING_REQUIRED("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/FilteringRequired"),
        INSECURE_ADDRESS("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InsecureAddress"),
        FORMAT_MISMATCH("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/FormatMismatch"),
        FORMAT_SECURITY_TOKEN("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/FormatSecurityToken"),
        ASYNCHRONOUS_REQUEST("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/AsynchronousRequest"),
        MISSING_VALUES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/MissingValues"),
        INVALID_VALUES("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValues"),
        INVALID_NAMESPACE("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidNamespace"),
        OPTION_SET("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/OptionSet");

        private final String uri;
        Detail(final String uri) { this.uri = uri; }
        public String toString() { return uri; }
    }

    public UnsupportedFeatureFault(final Detail... detail) {
        this(SOAP.createFaultDetail(null,
                detail == null ? null : (detail.length == 0 ? null : detail[0].toString()),
                null, null));
    }

    public UnsupportedFeatureFault(final Node... details) {
        super(Management.FAULT_ACTION_URI, UNSUPPORTED_FEATURE, UNSUPPORTED_FEATURE_REASON, details);
    }
}
