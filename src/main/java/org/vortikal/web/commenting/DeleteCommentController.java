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
package org.vortikal.web.commenting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import org.vortikal.repository.Comment;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

/**
 * Controller that removes a {@link Comment} from a {@link Resource}.
 * 
 */
public class DeleteCommentController extends AbstractController implements InitializingBean {

    private Repository repository;
    private String viewName;
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.viewName == null)
            throw new BeanInitializationException("Property 'viewName' must be set");
    }

    protected Repository getRepository() {
        return this.repository;
    }
    
    protected String getViewName() {
        return this.viewName;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        
        String uri = requestContext.getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, true);

        int id = -1;
        try {
            id = Integer.parseInt(request.getParameter("comment-id"));
        } catch (Throwable t) {
            throw new ResourceNotFoundException("No such comment");
        }

        List<Comment> comments = this.repository.getComments(token, resource);
        Comment comment = findComment(id, comments);
        if (comment == null) {
            throw new ResourceNotFoundException("No such comment");
        }
        this.repository.deleteComment(token, resource, comment);
        
        Map model = new HashMap();
        model.put("resource", resource);

        return new ModelAndView(this.viewName, model);
    }

    private Comment findComment(int id, List<Comment> comments) {
        for (Comment c: comments) {
            if (id == c.getID()) {
                return c;
            }
        }
        return null;
    }
}
