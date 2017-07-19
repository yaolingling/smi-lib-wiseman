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
 * $Id: EventDeliverToUnusableFault.java,v 1.1 2008-05-30 12:34:10 jfdenise Exp $
 */

package com.sun.ws.management;

import com.sun.ws.management.soap.SenderFault;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;

public class EventDeliverToUnusableFault extends SenderFault {
    
    public static final QName EVENT_DELIVER_TO_UNUSABLE = 
            new QName(Management.NS_URI, "EventDeliverToUnusable", Management.NS_PREFIX);
    public static final String EVENT_DELIVER_TO_UNUSABLE_REASON =
            "The event source cannot process the subscription because it cannot " +
            "connect to the event delivery endpoint as requested in the wse:Delivery element.";
    
    public EventDeliverToUnusableFault(final Node... details) {
        super(Management.FAULT_ACTION_URI, EVENT_DELIVER_TO_UNUSABLE,
                EVENT_DELIVER_TO_UNUSABLE_REASON, details);
    }
}
