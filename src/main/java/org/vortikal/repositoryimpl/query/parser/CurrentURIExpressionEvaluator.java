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
package org.vortikal.repositoryimpl.query.parser;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


public class CurrentURIExpressionEvaluator implements ExpressionEvaluator {
    
    private Log logger = LogFactory.getLog(this.getClass());
    private Repository repository;
    private String variableName = "currentURI";
    
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public boolean matches(String token) {
        return this.variableName.equals(token);
    }
    
    public String evaluate(String token) throws QueryException {

        if (this.repository == null) {
            throw new IllegalStateException("JavaBean property 'repository' not set");
        }


        if (!this.variableName.equals(token)) {
            throw new QueryException("Unknown query token: '" + token + "'");
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (requestContext != null && securityContext != null) {

            try {
                
                String securityToken = securityContext.getToken();
                String uri = requestContext.getResourceURI();
                Resource resource = this.repository.retrieve(securityToken, uri, true);
                if (!resource.isCollection()) {
                    resource = this.repository.retrieve(
                        securityToken, resource.getParent(), true);
                }
                return resource.getURI();
                
            } catch (Throwable t) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Unable to resolve current URI", t);
                }
            }
        }

        return token;
    }
}

