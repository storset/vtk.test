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
package vtk.repository.search.query;


public class UriPrefixQuery implements UriQuery {

    private String uri;
    private boolean inverted = false;
    private boolean includeSelf = true;
    
    public UriPrefixQuery(String uri) {
        this(uri, false);
    }

    public UriPrefixQuery(String uri, boolean inverted) {
        // Be backwards compatible with older behaviour on significance of trailing slash.
        // XXX: Note that the root URI '/' is a special case, it will not be included
        //      as part of URI prefix query results (only the children), unless
        //      this is explicitly set using #setIncludeSelf(boolean)
        if ("/".equals(uri)) {
            this.includeSelf = false;
        } else if (uri.endsWith("/")) {
            this.includeSelf = false;
            uri = uri.substring(0, uri.length()-1);
        }

        this.uri = uri;
        this.inverted = inverted;
    }
    
    public String getUri() {
        return this.uri;
    }

    public boolean isInverted() {
        return inverted;
    }
    
    public boolean isIncludeSelf() {
        return this.includeSelf;
    }
    
    public void setIncludeSelf(boolean includeSelf) {
        this.includeSelf = includeSelf;
    }
    
    @Override
    public Object accept(QueryTreeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName());
        sb.append(";uriPrefix = ").append(this.uri);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UriPrefixQuery other = (UriPrefixQuery) obj;
        if ((this.uri == null) ? (other.uri != null) : !this.uri.equals(other.uri)) {
            return false;
        }
        if (this.inverted != other.inverted) {
            return false;
        }
        if (this.includeSelf != other.includeSelf) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 97 * hash + (this.inverted ? 1 : 0);
        hash = 97 * hash + (this.includeSelf ? 1 : 0);
        return hash;
    }

}
