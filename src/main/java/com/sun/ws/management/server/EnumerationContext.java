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
 **$Log: not supported by cvs2svn $
 **Revision 1.17  2007/12/18 11:55:45  denis_rachal
 **Changes to ensure access to member variables in context are synchronized properly for multi-thread access.
 **
 **Revision 1.16  2007/12/06 06:43:36  denis_rachal
 **Issue number:  149
 **Obtained from:
 **Submitted by:  stanullo
 **Reviewed by:
 **
 **No immediate response after context expiration. Code added to wake up thread waiting for items. Unit test added to test that this now works and neither the Release or the Pull threads are blocked for any length of time.
 **
 **Revision 1.15  2007/12/05 13:24:45  denis_rachal
 **Added finalize code to release cached enumeration items.
 **
 **Revision 1.14  2007/11/30 14:32:38  denis_rachal
 **Issue number:  140
 **Obtained from:
 **Submitted by:  jfdenise
 **Reviewed by:
 **
 **WSManAgentSupport and WSEnumerationSupport changed to coordinate their separate threads when handling wsman:OperationTimeout and wsen:MaxTime timeouts. If a timeout now occurs during an enumeration operation the WSEnumerationSupport is notified by the WSManAgentSupport thread. WSEnumerationSupport saves any items collected from the EnumerationIterator in the context so they may be fetched by the client on the next pull. Items are no longer lost on timeouts.
 **
 **Tests were added to correctly test this functionality and older tests were updated to properly test timeout functionality.
 **
 **Additionally some tests were updated to make better use of the XmlBinding object and improve performance on testing.
 **
 **Revision 1.13  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: EnumerationContext.java,v 1.18 2008-01-17 15:19:09 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.dmtf.schemas.wbem.wsman._1.wsman.EnumerationModeType;

class EnumerationContext extends BaseContext {


    private final EnumerationIterator iterator;
    private final EnumerationModeType mode;
    private final List<EnumerationItem> passed;

    EnumerationContext(final XMLGregorianCalendar expiration,
            final Filter filter,
            final EnumerationModeType mode,
            final EnumerationIterator iterator,
            final ContextListener listener) {
        super(expiration, filter, listener);
        this.iterator = iterator;
        this.mode = mode;
        this.passed = new ArrayList<EnumerationItem>();
    }

    /**
     * Returns the EnumerationMode
     * @return the EnumerationModeType, null if the mode was not set
     */
    public EnumerationModeType getEnumerationMode() {
        return this.mode;
    }

    /**
     * Returns the iterator associated with this enumeration.
     *
     * @return the iterator associated with this enumeration
     */
    public EnumerationIterator getIterator() {
        return this.iterator;
    }
    
    public List<EnumerationItem> getItems() {
    	return this.passed;
    }

    public synchronized void setDeleted() {
    	if (isDeleted())
    		return;
    	super.setDeleted();
    	if (this.iterator != null) {
			synchronized (this.iterator) {
				this.iterator.notifyAll();
			}
		}
    }

    protected void finalize () throws Throwable {
        if (this.passed != null) {
        	this.passed.clear();
        }
        if (this.iterator != null) {
        	this.iterator.release();
        }
    }
}
