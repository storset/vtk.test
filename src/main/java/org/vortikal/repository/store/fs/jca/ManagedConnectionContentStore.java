/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.fs.jca;

import java.io.InputStream;

import javax.resource.ResourceException;

import org.vortikal.repository.store.ContentStore;
import org.vortikal.repository.store.DataAccessException;


public class ManagedConnectionContentStore implements ContentStore {

    private FileSystemConnectionFactory connectionFactory;
    
    public void setConnectionFactory(FileSystemConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    public void createResource(String uri, boolean isCollection) throws DataAccessException {

        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.createResource(uri, isCollection);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    

    public long getContentLength(String uri) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            return conn.getContentLength(uri);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    

    public void deleteResource(String uri) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.deleteResource(uri);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    

    public InputStream getInputStream(String uri) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            return conn.getInputStream(uri);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    

    public void storeContent(String uri, InputStream inputStream) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.storeContent(uri, inputStream);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    

    public void copy(String srcURI, String destURI) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.copy(srcURI, destURI);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
    
    
    public void move(String srcURI, String destURI) throws DataAccessException {
        FileSystemConnection conn = null;
        try {
            conn = this.connectionFactory.getConnection();
            conn.move(srcURI, destURI);
        } catch (ResourceException e) {
            throw new DataAccessException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (ResourceException e) {
                    throw new DataAccessException(e);
                }
            }
        }
    }
}
