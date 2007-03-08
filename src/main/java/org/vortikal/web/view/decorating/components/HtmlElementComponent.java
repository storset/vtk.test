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
package org.vortikal.web.view.decorating.components;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;
import org.vortikal.web.view.decorating.html.HtmlContent;
import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlNodeFilter;


public class HtmlElementComponent extends AbstractHtmlSelectComponent {

    private static Log logger = LogFactory.getLog(AbstractDecoratorComponent.class);

    private static final String PARAMETER_EXCLUDE = "exclude";
    private static final String PARAMETER_EXCLUDE_DESC = 
        "Comma-separated list of child element names to exclude (takes presedence over includes)";

    private static final String PARAMETER_INCLUDE = "include";
    private static final String PARAMETER_INCLUDE_DESC = 
        "Comma-separated list of child element names to include";

    private static final String PARAMETER_ENCLOSED = "enclosed";
    private static final String PARAMETER_ENCLOSED_DESC =
        "If the selected element should enclose the content, set this to 'true'";
    
    private static final String PARAMETER_SELECT_DESC = "The element to select";

    private static final String DESCRIPTION
        = "Outputs the contents of the element(s) identified by select";
    
    private String include;
    private String exclude;
    private Boolean enclosed;
    
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public void setEnclosed(boolean enclosed) {
        this.enclosed = Boolean.valueOf(enclosed);
    }
    
    
    public void processElements(List elements, DecoratorRequest request,
                                DecoratorResponse response) throws Exception {

        boolean enclosed = (this.enclosed != null) ?
                this.enclosed.booleanValue() : "true".equals(request.getStringParameter(PARAMETER_ENCLOSED));

        String include = (this.include != null) ?
            this.include : request.getStringParameter(PARAMETER_INCLUDE);

        Set includedElements = new HashSet();
        if (include != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                includedElements.add(splitValues[i]);
            }
        }

        String exclude = (this.exclude != null) ?
            this.exclude : request.getStringParameter(PARAMETER_EXCLUDE);

        Set excludedElements = new HashSet();
        if (exclude != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                excludedElements.add(splitValues[i]);
            }
        }

        Writer out = response.getWriter();

        for (int i = 0; i < elements.size(); i++) {
            boolean included = true;
            HtmlElement element = (HtmlElement) elements.get(i);
            
            Filter filter = new Filter(excludedElements, includedElements);
            if (enclosed) {
                out.write(element.getEnclosedContent(filter));
            } else {
                out.write(element.getContent(filter));
            }
        }
        out.flush();
        out.close();
    }

    private class Filter implements HtmlNodeFilter {

        private Set excluded, included;

        public Filter(Set excluded, Set included) {
            this.excluded = excluded;
            this.included = included;
        }

        public HtmlContent filterNode(HtmlContent node) {
            if (node instanceof HtmlElement) {
                HtmlElement element = (HtmlElement) node;
                if (excluded.contains(element.getName())) {
                    return null;
                } else if (include != null && !include.contains(element.getName())) {
                    return node;
                }
            }
            return node;
        }
    }
    


    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }

    protected Map getParameterDescriptionsInternal() {
        Map map = new HashMap();
        if (this.elementPath == null)
            map.put(PARAMETER_SELECT, PARAMETER_SELECT_DESC);
        if (this.include == null)
            map.put(PARAMETER_INCLUDE, PARAMETER_INCLUDE_DESC);
        if (this.exclude == null)
            map.put(PARAMETER_EXCLUDE, PARAMETER_EXCLUDE_DESC);
        if (this.enclosed == null)
            map.put(PARAMETER_ENCLOSED, PARAMETER_ENCLOSED_DESC);
        return map;
    }

}

