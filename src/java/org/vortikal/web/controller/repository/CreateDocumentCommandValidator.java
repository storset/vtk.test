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

import org.vortikal.security.PrincipalStore;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 */
public class CreateDocumentCommandValidator
  implements Validator, InitializingBean {

    private PrincipalStore principalStore;
    
    /**
     * @param principalStore The principalStore to set.
     */
    public void setPrincipalStore(PrincipalStore principalStore) {
        this.principalStore = principalStore;
    }
    
    /**
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    public boolean supports(Class clazz) {
        return (clazz == CreateDocumentCommand.class);

    }

    public void validate(Object command, Errors errors) {
        CreateDocumentCommand createDocumentCommand =
            (CreateDocumentCommand) command;

        if (createDocumentCommand.getCancel() != null) return;
        
        if (createDocumentCommand.getName() == null
            || createDocumentCommand.getName().trim().equals("")) {
            errors.rejectValue("name", "manage.create.document.missing.name",
                               "You must type a value");
        }
        
        if (createDocumentCommand.getSourceURI() == null
            || createDocumentCommand.getSourceURI().trim().equals("")) {
            errors.rejectValue("sourceURI",
                               "manage.create.document.missing.template",
                               "You must choose a document type");
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (principalStore == null) {
            throw new BeanInitializationException(
                "Property 'principalStore' cannot be null");
        }
    }

}
