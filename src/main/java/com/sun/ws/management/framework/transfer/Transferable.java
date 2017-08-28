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
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: Transferable.java,v $
 **Revision 1.2  2007/05/31 19:47:48  nbeers
 **Add HP copyright header
 **
 **
 * $Id: Transferable.java,v 1.2 2007/05/31 19:47:48 nbeers Exp $
 *
 */
package com.sun.ws.management.framework.transfer;

import com.sun.ws.management.Management;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.soap.SenderFault;
import com.sun.ws.management.transfer.InvalidRepresentationFault;

public interface Transferable {
    public void create(HandlerContext context,Management request,Management response) throws InvalidRepresentationFault;
    public void delete(HandlerContext context,Management request,Management response) throws SenderFault;
    public void get(HandlerContext context,Management request, Management response) throws SenderFault;
    public void put(HandlerContext context, Management  request, Management response) throws InvalidRepresentationFault, SenderFault;
 
}

