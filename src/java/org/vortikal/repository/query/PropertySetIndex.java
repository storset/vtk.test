/* Copyright (c) 2006, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.query;

import java.util.Iterator;

import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * <p>Defines an interface for modifying and inspecting the contents of 
 * an index of <code>PropertySet</code>s.</p>
 * 
 * <p>
 * Each <code>PropertySet</code> is primarily identified by its URI. In addition, 
 * implementations may provide an auxilliary UUID, which should be uniqe for any given <code>
 * PropertySet</code> <em>and also independent of time</em> (UUIDs should thus never 
 * be re-used, even for new nodes at the same URI in the same index instance).
 *  
 * This might be used in the implementation for increased efficiency when
 * modifying or searching an index hierarchially and for consistency reasons. 
 * The benefits of using UUIDs is entirely implementation specific. This interface
 * does not state how a UUID should be generated for a <code>PropertySet</code>.
 * This is up to the implementation.
 * </p>
 * 
 * <p>
 * A node (<code>PropertySet</code>) can be a parent of other nodes 
 * (collection). This relationship, however, is only reflected in the URIs themselves.
 * The index does not need to strictly enforce that any given node
 * has an existing parent. In addition to this, the implmementation does not 
 * have to ensure that there are no duplicates for a given URI. This is for the sake
 * of efficiency.
 * </p>
 * 
 * <p>
 * The {@link #lock()}, {@link #unlock()} and {@link #commit()} methods
 * should provide the possibility of executing a "semi-transactional" set
 * operations that cannot be mixed with other write operations from other threads
 * at the same time. Any modifying operation is not guaranteed to be visible by
 * other index users before {@link commit()} has been called.
 * </p>
 *
 * 
 * @author oyviste
 */
public interface PropertySetIndex {
    
    /**
     * Add <code>PropertySet</code> to index.
     * @param propertySet
     * @throws IndexException
     */
    public void addPropertySet(PropertySet propertySet) throws IndexException;
    
    /**
     * Delete any <code>PropertySet</code> with the given URI. If there
     * are multiple property sets with the same URI, then they should all be
     * deleted.
     * 
     * @param uri
     * @return The number of instances deleted.
     * @throws IndexException
     */
    public int deletePropertySet(String uri) throws IndexException;
    
    /**
     * Delete <code>PropertySet</code> with the given auxilliary UUID.
     * Optional.
     * 
     * @param uuid
     * 
     * @return The number of instances deleted.
     * 
     * @throws IndexException
     */
    public int deletePropertySetByUUID(String uuid) throws IndexException;
    
    /**
     * Delete the <code>PropertySet</code> at the given root URI and all its
     * descendants. This method should also delete URI duplicates in the tree, 
     * if there are any.
     * 
     * @param rootUri
     * @return The number of deleted instances. If it's not 0 or 1, 
     *         then something is very wrong with the implementation.
     *         
     * @throws IndexException
     */
    public int deletePropertySetTree(String rootUri) throws IndexException;
    
    /**
     * Delete the <code>PropertySet</code> with the given auxilliary UUID and all its
     * descendants.
     * Optional.
     * 
     * 
     * @param rootUuid
     * @return
     * @throws IndexException
     */
    public int deletePropertySetTreeByUUID(String rootUuid) throws IndexException;
    
    /**
     * Get the <code>PropertySet</code> at the given.
     * 
     * If multiple <code>PropertySet</code> instances have been added for the URI, 
     * it is undefined which of them is returned using this method. The 
     * {@link #iterator} method should be used to detect such anomalies.
     * 
     * @param uri
     * @return
     * @throws IndexException If no <code>PropertySet</code> was found in the index
     */
    public PropertySet getPropertySet(String uri) throws IndexException;
    
    /**
     * Get an {@link java.util.Iterator} over all <code>PropertySet</code> instances
     * in index.
     * 
     * The iteration is ordered by URI lexicographically. Any duplicates are included, 
     * and should <em>directly</em> follow each other.
     * 
     * @return
     * @throws IndexException
     */
    public Iterator orderedIterator() throws IndexException;
    
    /**
     * Get an {@link java.util.Iterator} over all <code>PropertySet</code> instances
     * in the sub-tree given by the root URI.
     * 
     * The iteration is ordered by URI lexicographically. Any duplicates are included
     * and should <em>directly</em> follow each other.
     * 
     * @param rootUri
     * @return
     * @throws IndexException
     */
    public Iterator orderedSubtreeIterator(String rootUri) throws IndexException;
    
    /**
     * Close an <code>Iterator</code> obtained with either 
     * {@link #orderedIterator()} or {@link #orderedSubtreeIterator(String)} to free index resources.
     * 
     * @param iterator
     * @throws IndexException
     */
    public void close(Iterator iterator) throws IndexException;
    
    /**
     * Clear contents of index.
     * 
     * @throws IndexException
     */
    public void clear() throws IndexException;
    
    /**
     * Obtain index mutex write lock.
     * 
     * @return
     * @throws IndexException
     */
    public boolean lock() throws IndexException;
    
    /**
     * Release index mutex write lock.
     * @throws IndexException
     */
    public void unlock() throws IndexException;
 
    /**
     * Commit any changes
     * @throws IndexException
     */
    public void commit() throws IndexException;
    
}
