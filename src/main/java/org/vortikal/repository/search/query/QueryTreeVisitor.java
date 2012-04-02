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
package org.vortikal.repository.search.query;

/**
 * Simple generic visitor interface for concrete query node implementations. 
 *
 */
public interface QueryTreeVisitor {

    public Object visit(AndQuery andQuery, Object data);
    
    public Object visit(OrQuery orQuery, Object data);

    public Object visit(NamePrefixQuery npQuery, Object data);
    
    public Object visit(NameRangeQuery nrQuery, Object data);
    
    public Object visit(NameWildcardQuery nwQuery, Object data);

    public Object visit(NameTermQuery ntQuery, Object data);
    
    public Object visit(PropertyExistsQuery peQuery, Object data);
    
    public Object visit(PropertyPrefixQuery ppQuery, Object data);
    
    public Object visit(PropertyRangeQuery prQuery, Object data);
    
    public Object visit(PropertyTermQuery ptQuery, Object data);
    
    public Object visit(PropertyWildcardQuery pwQuery, Object data);
    
    public Object visit(TypeTermQuery ttQuery, Object data);
    
    public Object visit(UriDepthQuery udQuery, Object data);
    
    public Object visit(UriPrefixQuery upQuery, Object data);
    
    public Object visit(UriSetQuery usQuery, Object data);
    
    public Object visit(UriTermQuery utQuery, Object data);
    
    public Object visit(ACLExistsQuery aclQuery, Object data);
    
    public Object visit(ACLInheritedFromQuery aclIHFQuery, Object data);
    
    public Object visit(ACLReadForAllQuery query, Object data);
    
    public Object visit(MatchAllQuery query, Object data);
    
}
