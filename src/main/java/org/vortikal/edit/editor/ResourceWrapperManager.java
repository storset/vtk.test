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
package org.vortikal.edit.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;

public class ResourceWrapperManager {

    private Repository repository;
    private HtmlPageParser htmlParser;
    private HtmlPageFilter htmlPropsFilter;
    private EditablePropertyProvider editPropertyProvider = new ResourceTypeEditablePropertyProvider();
    private ResourceTypeDefinition contentResourceType;

    
    public HtmlPageParser getHtmlParser() {
        return this.htmlParser;
    }
    
    public HtmlPageFilter getHtmlPropsFilter() {
        return htmlPropsFilter;
    }

    public ResourceWrapper createResourceWrapper(String uri) throws IOException, Exception {
        ResourceWrapper wrapper = new ResourceWrapper(this);
        
        populateWrapper(wrapper, uri, true);
        
        return wrapper;
    
    }
    
    public ResourceWrapper createResourceWrapper() throws IOException, Exception {
        String uri = RequestContext.getRequestContext().getResourceURI();
        return createResourceWrapper(uri);
    }

    public ResourceEditWrapper createResourceEditWrapper() throws IOException, Exception {
        ResourceEditWrapper wrapper = new ResourceEditWrapper(this);
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        populateWrapper(wrapper, uri, false);
        
        return wrapper;
    }

    private void populateWrapper(ResourceWrapper wrapper, String uri, boolean forProcessing) throws IOException,
            Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        
        Resource resource = this.repository.retrieve(token, uri, forProcessing);
        if (resource.isOfType(this.contentResourceType)) {
            InputStream is = this.repository.getInputStream(token, uri, forProcessing);
        
            byte[] bytes = StreamUtil.readInputStream(is);
        
            //String content = new String(bytes, resource.getCharacterEncoding());
            HtmlPage content = this.htmlParser.parse(new ByteArrayInputStream(bytes), resource.getCharacterEncoding());
        
            wrapper.setContent(content);
        }
        wrapper.setContentProperties(this.editPropertyProvider.getContentProperties(resource));
        wrapper.setExtraContentProperties(this.editPropertyProvider.getExtraContentProperties(resource));
        wrapper.setResource(resource);
    }


    public void store(ResourceEditWrapper wrapper) throws IOException {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = wrapper.getResource();
    
        if (wrapper.isPropChange()) {
            resource = this.repository.store(token, resource);
        }

        if (wrapper.isContentChange()) {
            byte[] bytes = wrapper.getContent().getStringRepresentation().getBytes(resource.getCharacterEncoding());
            this.repository.storeContent(token, uri, new ByteArrayInputStream(bytes));
        }
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    @Required public void setContentResourceType(ResourceTypeDefinition contentResourceType) {
        this.contentResourceType = contentResourceType;
    }

    @Required public void setHtmlPropsFilter(HtmlPageFilter htmlPropsFilter) {
        this.htmlPropsFilter = htmlPropsFilter;
    }

    public void unlock() throws ReadOnlyException, ResourceNotFoundException, AuthorizationException, FailedDependencyException, ResourceLockedException, IllegalOperationException, AuthenticationException, IOException {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        this.repository.unlock(token, uri, null);
    }

    public void lock() throws ReadOnlyException, ResourceNotFoundException, AuthorizationException, ResourceLockedException, AuthenticationException, IOException {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        this.repository.lock(token, uri, principal.getQualifiedName(), "0", 600, null);
    }

}
