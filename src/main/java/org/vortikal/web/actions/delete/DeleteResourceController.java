/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.actions.delete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

@SuppressWarnings("deprecation")
public class DeleteResourceController extends SimpleFormController {

    /* TODO: change class name to something that says it is used when standing on resource */

    private String cancelView;
    private DeleteHelper deleteHelper;

    protected Object cmd;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();
        Resource resource = repository.retrieve(token, requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, principal);
        String name = resource.getName();

        this.cmd = new DeleteCommand(name, url);
        return this.cmd;
    }

    protected ModelAndView onSubmit(Object command) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();

        DeleteCommand deleteCommand = (DeleteCommand) command;

        if (deleteCommand.getCancelAction() != null) {
            deleteCommand.setDone(true);
            return new ModelAndView(this.cancelView);
        }

        Resource resource = repository.retrieve(token, uri.getParent(), false);

        // File that for some reason failed on delete. Separated by a
        // key (String) that specifies type of failure and identifies
        // path to resource that failed.
        Map<String, List<Path>> failures = new HashMap<String, List<Path>>();

        this.deleteHelper.deleteResource(repository, token, uri, true, failures);
        this.deleteHelper.addFailureMessages(failures, requestContext);
        if (!failures.isEmpty()) {
            return new ModelAndView(super.getFormView());
        }

        deleteCommand.setDone(true);

        model.put("resource", resource);

        return new ModelAndView(this.getSuccessView(), model);
    }

    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }

    @Required
    public void setDeleteHelper(DeleteHelper deleteHelper) {
        this.deleteHelper = deleteHelper;
    }
}
