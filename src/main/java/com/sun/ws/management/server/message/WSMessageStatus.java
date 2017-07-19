/*
 * Copyright (C) 2007 Hewlett-Packard Development Company, L.P.
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
 ** Copyright (C) 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Denis Rachal (denis.rachal@hp.com)
 **
 */
package com.sun.ws.management.server.message;

/**
 * This class tracks the status of a request message on the server side.
 * Whether the message has been canceled and if the request has been committed.
 * 
 * @author denis.rachal@hp.com
 *
 */
public class WSMessageStatus {

    private boolean cancelled = false;
    private boolean committed = false;
    
    /**
     * Sets this request to canceled state. If the request has already been set
     * to committed an IllegalStateException will be thrown.
     * 
     * @throws IllegalStateException
     */
	public void cancel() throws IllegalStateException {
		synchronized (this) {
			if (this.committed == true)
				throw new IllegalStateException("Already committed.");
			this.cancelled = true;
		}
	}

    /**
     * Sets this request to committed state. If the request has already been set
     * to canceled an IllegalStateException will be thrown.
     * 
     * @throws IllegalStateException
     */
	public void commit() throws IllegalStateException {
		synchronized (this) {
			if (this.cancelled == true)
				throw new IllegalStateException("Already cancelled.");
			this.committed = true;
		}		
	}

    /**
     * Check to see if this request has been canceled.
     * This indicates to the service to roll back any uncommitted work.
     * 
     * @return boolean indicating if the request has been canceled.
     */
	public boolean isCanceled() {
		synchronized (this) {
		    return this.cancelled;
		}
	}

    /**
     * Check to see if this request has been committed.
     * If marked committed the request has been completed by the service.
     * This does not indicate if the response has been sent to the client.
     * 
     * @return boolean indicating if the request has been canceled.
     */
	public boolean isCommitted() {
		synchronized (this) {
		    return this.committed;
		}
	}
}
