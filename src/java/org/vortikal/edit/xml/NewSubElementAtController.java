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
package org.vortikal.edit.xml;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;

import org.jdom.Element;
import org.springframework.web.servlet.ModelAndView;




/**
 * Controller that adds a new subelement to an already expanded
 * element.
 *
 * @version $Id$
 */
public class NewSubElementAtController extends AbstractXmlEditController {

    
    protected ModelAndView handleRequestInternal(
        HttpServletRequest request, HttpServletResponse response,
        EditDocument document, SchemaDocumentDefinition documentDefinition) {

        Map model = new HashMap();
        String mode = document.getDocumentMode();

        if (mode.equals("edit") || mode.equals("newElementAt")) {

            String path = request.getParameter("path");
            String elementName = request.getParameter("elementname");

            document.addContentsToElement(
                document.getEditingElement(),
                getRequestParameterMap(request),
                documentDefinition);

            documentDefinition.translateToEditingElement(
                document.getEditingElement());

            Element element = new Element(elementName);
            document.putElementByPath(path, element);
            documentDefinition.buildElement(element);
            return new ModelAndView(viewName, model);

        }
        return null;
    }



    protected void setFormActionServiceURL(Map model, EditDocument document) {
        Resource resource = document.getResource();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();

        // FIXME: what if ?
        String serviceURL = null;
        
        if (document.getDocumentMode().equals("edit")) {
            serviceURL = editElementDoneService.constructLink(resource, principal);
        } else if (document.getDocumentMode().equals("newElementAt")) {
            serviceURL = newElementAtService.constructLink(resource, principal);
        }

        if (serviceURL != null)
            setXsltParameter(model, "formActionServiceURL", serviceURL);
    }

}
