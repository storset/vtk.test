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
package org.vortikal.web.controller.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Repository;
import org.vortikal.security.PrincipalStore;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


public class CreateCollectionCommandValidator implements Validator, InitializingBean {

    private static Log logger = LogFactory.getLog(CreateCollectionCommandValidator.class);

    private Repository repository = null;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    
    public void afterPropertiesSet() throws Exception {
        if (repository == null) {
            throw new BeanInitializationException(
                "Property 'repository' cannot be null");
        }
    }


    public boolean supports(Class clazz) {
        return (clazz == CreateCollectionCommand.class);
    }


    public void validate(Object command, Errors errors) {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateCollectionCommand createCollectionCommand =
            (CreateCollectionCommand) command;
        if (createCollectionCommand.getCancelAction() != null) {
            return;
        }
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        String name = createCollectionCommand.getName();       
        if (null == name || "".equals(name.trim())) {
            errors.rejectValue("name",
                               "manage.create.collection.missing.name",
                               "A name must be provided for the collection");
            return;
        }

        if (name.indexOf("/") >= 0) {
            // TODO: move general resource validation code into a
            // utility class.

            errors.rejectValue("name",
                               "manage.create.collection.invalid.name",
                               "This is an invalid collection name");
        }
            
        String newURI = uri;
        if (!"/".equals(uri)) newURI += "/";
        newURI += name;

        try {
            boolean exists = repository.exists(token, newURI);
            if (exists) {
                errors.rejectValue("name",
                                   "manage.create.collection.exists",
                                   "A collection with this name already exists");
            }
        } catch (Exception e) {
            logger.warn("Unable to validate collection creation input", e);
        }

    }

}
