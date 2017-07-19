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
 **$Log: not supported by cvs2svn $
 ** 
 *
 * $Id: EnumerationPullIterator.java,v 1.4 2007-05-30 20:31:04 nbeers Exp $
 */
package com.sun.ws.management.server;

import com.sun.ws.management.enumeration.Enumeration;

/**
 * This is an extension of the EnumerationIterator interface that provides
 * the application access to the request and response messages.
 *
 */
public interface EnumerationPullIterator extends EnumerationIterator {

	public void startPull(final HandlerContext context, final Enumeration request);
	
	public void endPull(final Enumeration response);
}
