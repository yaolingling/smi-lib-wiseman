/*
 * Copyright 2006-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.ws.management.server;

import com.sun.ws.management.UnsupportedFeatureFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.server.message.WSEnumerationRequest;
import com.sun.ws.management.soap.FaultException;

/**
 *
 * @author jfdenise
 */
public interface WSEnumerationIteratorFactory {
    /**
     * EnumerationIterator creation.
     * 
     * @param context the HandlerContext
     * @param request the Enumeration request that this iterator is to fufill
     * @param includeItem if true the requester wants the item returned, otherwise
     * just the EPR if includeEPR is true
     * @param includeEPR if true the requestor wants the EPR for each item returned, otherwise
     * just the item if includeItem is true. If EPRs are not supported by the iterator,
     * the iterator should throw an UnsupportedFeatureFault.
     * 
     * @throws com.sun.ws.management.UnsupportedFeatureFault If EPRs are not supported.
     * @throws com.sun.ws.management.soap.FaultException If a WS-MAN protocol related exception occurs.
     * @return An enumeration iterator for the request
     */
    public EnumerationIterator newIterator(final HandlerContext context, 
			final WSEnumerationRequest request, 
			final boolean includeItem,
			final boolean includeEPR)
    throws UnsupportedFeatureFault, FaultException;
}
