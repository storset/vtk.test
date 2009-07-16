/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.List;

import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;

/**
 * This interface contains methods for acquiring metadata about a system's 
 * principals. 
 *
 */
public interface PrincipalMetadataDAO {

    /**
     * Get metadata instance for principal with the given
     * fully qualified name (username/id+domain).
     * 
     * 
     * @param qualifiedName The fully qualified name of the principal to get the description for.
     * @return A <code>PrincipalMetadata</code> instance with description
     *         or <code>null</code> if no metadata could be found.
     * 
     */
    public PrincipalMetadata getMetadata(String qualifiedName);
    
    /**
     * Get metadata instance for given principal instance.
     * 
     * @param principal The <code>Principal</code> instance to get the description for.
     * @return A <code>PrincipalMetadata</code> instance or <code>null</code> if none found.
     * 
     */
    public PrincipalMetadata getMetadata(Principal principal);
    
    /**
     * Searches for principals which satisfy the supplied search criteria, represented as an 
     * implementation-specific <code>String</code>.
     * 
     * @param searchString String to use in search
     * @param type Type of {@link org.vortikal.security.Principal} to search for.
     * @see org.vortikal.security.Principal.Type
     * 
     * @return List of metadata-instances (<code>PrincipalMetadata</code>) 
     *         for each principal that satisfies the search criteria.
     */
    public List<PrincipalMetadata> search(String searchString, Type type);


}
