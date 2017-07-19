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
 **Revision 1.16  2007/12/18 11:55:46  denis_rachal
 **Changes to ensure access to member variables in context are synchronized properly for multi-thread access.
 **
 **Revision 1.15  2007/11/16 17:03:01  jfdenise
 **Added some checks and handle stop then start of the Timer.
 **
 **Revision 1.14  2007/11/16 15:12:13  jfdenise
 **Fix for bug 147 and 148
 **
 **Revision 1.13  2007/09/18 13:06:56  denis_rachal
 **Issue number:  129, 130 & 132
 **Obtained from:
 **Submitted by:
 **Reviewed by:
 **
 **129  ENHANC  P2  All  denis_rachal  NEW   Need support for ReNew Operation in Eventing
 **130  DEFECT  P3  x86  jfdenise  NEW   Should return a boolean variable result not a constant true
 **132  ENHANC  P3  All  denis_rachal  NEW   Make ServletRequest attributes available as properties in Ha
 **
 **Added enhancements and fixed issue # 130.
 **
 **Revision 1.12  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: BaseContext.java,v 1.17 2008-01-17 15:19:09 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BaseContext {
	
	private class BaseNodeList implements NodeList {

		private final ArrayList<Node> list;

		protected BaseNodeList() {
			list = new ArrayList<Node>();
		}
		
		protected BaseNodeList(int size) {
			list = new ArrayList<Node>(size);
		}
		
		public int getLength() {
			return list.size();
		}

		public Node item(int index) {
			return list.get(index);
		}
		
		protected void add(Node node) {
			list.add(node);
		}
	}
    
    private final Filter filter;
    private final ContextListener listener;
    
    private XMLGregorianCalendar expiration;
    private boolean deleted;
    
    public BaseContext(final XMLGregorianCalendar expiry,
            final Filter filter,
	        final ContextListener listener) {
        
        this.expiration = expiry;
        this.filter = filter;
        this.deleted = false;
        this.listener = listener;
    }
    
    public String getExpiration() {
    	if(expiration == null) return null;
        return expiration.toXMLFormat();
    }
    
    public ContextListener getListener() {
    	return this.listener;
    }
    
    public Filter getFilter() {
    	return this.filter;
    }
    
    public synchronized Date getExpirationDate() {
        if(expiration == null) return null;
        return expiration.toGregorianCalendar().getTime();
    }
    
    public synchronized boolean isExpired(final XMLGregorianCalendar now) {
        if (expiration == null) {
            // no expiration defined, never expires
            return false;
        }
        return now.compare(expiration) > 0;
    }
    
    public synchronized void renew(final XMLGregorianCalendar expires) {
    	this.expiration = expires;
    }
    
    public synchronized boolean isDeleted() {
    	return deleted;
    }
    
    public synchronized void setDeleted() {
    	this.deleted = true;
    }
    
    public NodeList evaluate(final Node content) throws Exception {
        // pass-thru if no filter is defined
        if (filter != null) {
            return filter.evaluate(content);
        } else {
        	final BaseNodeList list = new BaseNodeList(1);
        	list.add(content);
            return list;
        }
    }
}
