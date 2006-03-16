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
package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.io.InputStream;

import org.vortikal.repository.IllegalOperationException;

/**
 * Defines a pure content store. It is organized hierachically and 
 * resource nodes are adressed by their URIs. The behaviour shall be equal
 * to that of a common file system. Every node must have an 
 * existing parent collection resource node, except the root node. 
 * The root collection node shall always exist, and should not need to be 
 * created upon intialization.
 * 
 */
public interface ContentStore {

    public void createResource(String uri, boolean isCollection)
            throws IOException;

    public long getContentLength(String uri) throws IllegalOperationException;

    public void deleteResource(String uri);

    public InputStream getInputStream(String uri) throws IOException;

    /**
     * Store content in the resource given by the URI.
     * The supplied <code>InputStream</code> should be closed by this
     * method, after it has been read.
     * 
     * @param uri
     * @param inputStream
     * @throws IOException
     */
    public void storeContent(String uri, InputStream inputStream) throws IOException;

    public void copy(String srcURI, String destURI) throws IOException;
    
}