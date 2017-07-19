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
 * $Id: EnumerationIterator.java,v 1.12 2007-05-30 20:31:05 nbeers Exp $
 */

package com.sun.ws.management.server;


/**
 * The inteface to be presented by a data source that would like to be
 * enumerated by taking advantage of the functionality present in
 * {@link EnumerationSupport EnumerationSupport}.
 *
 * @see EnumerationSupport
 */
public interface EnumerationIterator {

    /**
     * Estimate the total number of elements available.
     *
     * @return an estimate of the total number of elements available
     * in the enumeration.
     * Return a negative number if an estimate is not available.
     */
    int estimateTotalItems();

    /**
     * Indicates if the iterator has already been filtered.
     * This indicates that further filtering is not required
     * by the framwork.
     *
     * @return {@code true} if the iterator has already been filtered,
     * {@code false} otherwise.
     */
    boolean isFiltered();

    /**
     * Indicates if there are more elements remaining in the iteration.
     *
     * @return {@code true} if there are more elements in the iteration,
     * {@code false} otherwise.
     */
    boolean hasNext();

    /**
     * Supply the next element of the iteration. This is invoked to
     * satisfy a {@link org.xmlsoap.schemas.ws._2004._09.enumeration.Pull Pull}
     * request.
     *
     * @return an {@link EnumerationItem Element} that is used to
     * construct proper responses for a
     * {@link org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse PullResponse}.
     */
    EnumerationItem next();

    /**
     * Release any resources being used by the iterator. Calls
     * to other methods of this iterator instance will exhibit
     * undefined behaviour, after this method completes.
     */
    void release();

}
