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
 * $Id: EventingIterator.java,v 1.3 2007-05-30 20:31:04 nbeers Exp $
 */
package com.sun.ws.management.server;

import java.util.concurrent.ArrayBlockingQueue;

import com.sun.ws.management.enumeration.InvalidEnumerationContextFault;

public class EventingIterator implements EnumerationIterator {
	
	private boolean isFiltered;
	private ArrayBlockingQueue<EnumerationItem> queue;

	/**
	 * Constructor for EventingIterator. This iterator handles
	 * managing events for pull style eventing.
	 * @param db 
	 * 
	 * @param isFiltered indicates events in this iterator have already
	 *        been filtered by the application and do not require
	 *        additional filtering.
	 * @param queueSize size of the queue. If an event is added when the
	 *        queue is full, the event on the top of the queue is deleted
	 *        and the new event is added at the end of the queue.
	 */
	protected EventingIterator(final boolean isFiltered,
			                   final int queueSize) {
			
		// parse request object to retrieve filter parameters entered.
		this.isFiltered = isFiltered;
		this.queue = new ArrayBlockingQueue<EnumerationItem>(queueSize);
			
	}

	public int estimateTotalItems() {
		if (this.queue == null) {
			return -1;
		}
		return this.queue.size();
	}

	public boolean hasNext() {
		return true;
	}

	public boolean isFiltered() {
		return this.isFiltered;
	}

	public EnumerationItem next() {
		if (this.queue == null) {
			throw new InvalidEnumerationContextFault();
		}
		synchronized (this.queue) {
			return this.queue.poll();
		}
	}

	public void release() {
		if (this.queue == null) {
			return;
		}
		synchronized (this.queue) {
			this.queue = null;
		}
	}

	public boolean add(EnumerationItem item) {
		if (this.queue == null) {
			return false;
		}
		synchronized (this) {
			boolean result = true;
			synchronized (this.queue) {
				if (this.queue.offer(item) == false) {
					// Remove item at the head of the queue
					this.queue.poll();
					result = this.queue.offer(item);
				}
			}
			// Notify anyone waiting on data in this iterator
			this.notifyAll();
			return result;
		}
	}
}
