/* Copyright (c) 2004, University of Oslo, Norway
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
/*
 * Created on 05.jul.2004
 *
 */
package org.vortikal.web.service;

import org.vortikal.repository.Resource;

/**
 *
 */
public class ResourceURIAssertion extends AssertionSupport implements ResourceAssertion {

    private String uri = "";
    private boolean inverted = false;
    

    /**
     * @return Returns the uri.
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri The uri to set.
     */
    public void setUri(String uri) {
        if (uri == null) throw new IllegalArgumentException(
            "Property 'uri' cannot be null");
    
        this.uri = uri;
    }
    
    /**
     * Gets the value of inverted
     *
     * @return the value of inverted
     */
    public boolean isInverted() {
        return this.inverted;
    }

    /**
     * Sets the value of inverted
     *
     * @param inverted Value to assign to this.inverted
     */
    public void setInverted(boolean inverted)  {
        this.inverted = inverted;
    }


    public boolean matches(Resource resource) {
        if (this.inverted) {
            return ! uri.equals(resource.getURI());
        }
        return uri.equals(resource.getURI());
    }

    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof ResourceURIAssertion) {
            ResourceURIAssertion other = (ResourceURIAssertion) assertion;
            if (!this.inverted && !other.isInverted()) {
                return ! (this.uri.equals(other.getUri()));
                
            } else if (this.inverted && !other.isInverted() ||
                       !this.inverted && other.isInverted()) {
                return this.uri.equals(other.getUri());
            }
        }
        return false;
    }

    /** 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        if (this.inverted) {
            sb.append("; URI != ").append(this.uri);
        } else {
            sb.append("; URI = ").append(this.uri);
        }
        return sb.toString();
    }

}
