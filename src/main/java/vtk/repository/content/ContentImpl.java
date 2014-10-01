/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package vtk.repository.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import vtk.repository.Path;
import vtk.repository.resourcetype.Content;
import vtk.repository.store.ContentStore;

/**
 * Implementation of <code>vtk.repository.resourcetype.Content</code>
 * interface.
 * 
 * NOTE: returned representations are <em>not</em> cloned. Modifications from the
 * outside will remain permanent for any given representation.
 * 
 * The <code>java.io.InputStream</code> representation is not cached, a new stream
 * is returned every time. This is the only exception.
 * 
 * Representations are created and cached lazily, when requested.
 * 
 */
public class ContentImpl implements Content {

    private final ContentStore contentStore;
    private final Path uri;
    private final ContentRepresentationRegistry contentRegistry;
    private final Map<Class<?>, Object> cachedRepresentations;
    
    public ContentImpl(Path uri, ContentStore contentStore,
                       ContentRepresentationRegistry contentRegistry) {
        this.cachedRepresentations = new HashMap<Class<?>, Object>();
        this.contentStore = contentStore;
        this.contentRegistry = contentRegistry;
        this.uri = uri;
    }
    
    @Override
    public <T> T getContentRepresentation(Class<T> clazz) throws Exception {
        // Make sure we have original content from inputstream 
        // before we do anything else. 
        
        // We don't cache InputStream representations
        if (clazz == java.io.InputStream.class) {
            return (T)this.contentStore.getInputStream(uri); 
        }
        
        Object representation = this.cachedRepresentations.get(clazz);
        if (representation == null) {
            // Lazy load representation
            try {
                InputStream pw  = this.contentStore.getInputStream(uri);
                representation = this.contentRegistry.createRepresentation(clazz, pw);
                
            } finally {
                // XXX hmm, might leak if a ContentFactory doesn't close it...
//                 if (inputStream != null) {
//                     inputStream.close();
//                 }
            }
            // Don't cache large buffers:
            if (clazz == byte[].class) {
                if (((byte[]) representation).length <= 1000000) {
                    this.cachedRepresentations.put(clazz, representation);
                }
            } else {
                this.cachedRepresentations.put(clazz, representation);
            }
        }
        
        return (T)representation;
    }
    
    @Override
    public InputStream getContentInputStream() throws IOException {
        try {
            return getContentRepresentation(java.io.InputStream.class);
        } catch (Exception e) {
            throw new IOException("Unable to create input stream representation: " + e.getMessage());
        }
    }
    
    @Override
    public long getContentLength() throws IOException {
        return this.contentStore.getContentLength(uri);
    }
 
}