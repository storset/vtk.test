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
package org.vortikal.repository;


public class Namespace {

    private final static String CUSTOM_NAMESPACE_URI = "http://www.uio.no/vortex/custom-properties";

    public final static Namespace DEFAULT_NAMESPACE = new Namespace(null, null);
    public final static Namespace CUSTOM_NAMESPACE = new Namespace("custom", 
            CUSTOM_NAMESPACE_URI);
    
    private String prefix;
    private String url;

    public Namespace(String prefix, String url) {
        this.prefix = prefix;
        this.url = url;
    }

    public static Namespace getNamespace(String uri) {
        if (uri == null) return DEFAULT_NAMESPACE;
        if (uri.equals(CUSTOM_NAMESPACE_URI)) return CUSTOM_NAMESPACE;
        
        return new Namespace(uri, uri);
    }
    
    public String getPrefix() {
        return this.prefix;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof Namespace)) return false;
        Namespace ns = (Namespace) obj;
        
        String thisUrl = (this.url == null) ? "" : this.url;
        String thatUrl = (ns.getUrl() == null) ? "" : ns.getUrl();
        String thisPrefix = (this.prefix == null) ? "" : this.prefix;
        String thatPrefix = (ns.getPrefix() == null) ? "" : ns.getPrefix();

        return thisUrl.equals(thatUrl) && thisPrefix.equals(thatPrefix);
    }
    
    public int hashCode() {
        String url = this.url == null ? "" : this.url;
        String prefix = this.prefix == null ? "" : this.prefix;
        
        return url.hashCode() + prefix.hashCode();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (this.prefix != null) {
            sb.append(this.prefix).append(":");
        }

        if (this.url == null) {
            sb.append("DEFAULT");
        }

        sb.append(this.url);
        return sb.toString();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
}
