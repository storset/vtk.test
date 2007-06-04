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
package org.vortikal.repository.resourcetype.property;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;

import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.Tag;


public class HtmlTitleElementEvaluator implements ContentModificationPropertyEvaluator {

    private PropertyTypeDefinition characterEncodingPropDef;

    private ContentModificationPropertyEvaluator characterEncodingEvaluator;
    private String defaultEncoding;
    

    private static Log logger = LogFactory.getLog(HtmlTitleElementEvaluator.class);

    
    public void setCharacterEncodingPropertyDefinition(PropertyTypeDefinition characterEncodingPropDef) {
        this.characterEncodingPropDef = characterEncodingPropDef;
    }
    

    public void setCharacterEncodingEvaluator(ContentModificationPropertyEvaluator characterEncodingEvaluator) {
        this.characterEncodingEvaluator = characterEncodingEvaluator;
    }
    
    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {
        
        InputStream stream = null;        
        String encoding = determineCharacterEncoding(principal, property, ancestorPropertySet, content, time);

        try {
            Source source = null;
            stream = (InputStream) content.getContentRepresentation(InputStream.class);
            source = new Source(new InputStreamReader(stream, encoding));

            Element titleElement = source.findNextElement(0, Tag.TITLE);
            if (titleElement == null) {
                return false;
            }
            String title = CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
            if ("".equals(title.trim())) {
                return false;
            }

            property.setStringValue(title);

            return true;
        } catch (Exception e) {
            logger.warn("Unable to evaluate title of HTML resource '"
                        + ancestorPropertySet.getURI() + "'", e);
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) { }
            }
        }
    }
    
    private String determineCharacterEncoding(Principal principal, Property property,
                                              PropertySet ancestorPropertySet, Content content, Date time) {
        
        String encoding = null;
        if (this.characterEncodingEvaluator != null) {            
            
            try {
                Property dummyProp = (Property) property.clone();
                boolean evaluated = this.characterEncodingEvaluator.contentModification(
                    principal, dummyProp, ancestorPropertySet, content, time);
                if (evaluated) {
                    encoding = dummyProp.getStringValue();
                }

            } catch (Exception e) { }
        }

        if (this.characterEncodingPropDef == null) {
            return encodingValue(encoding);
        }

        Property encProperty = ancestorPropertySet.getProperty(this.characterEncodingPropDef);
        if (encProperty != null) {
            try {
                encoding = encProperty.getStringValue();
                java.nio.charset.Charset.forName(encoding);
            } catch (Exception e) { }
        }
        return encodingValue(encoding);
    }
    
    private String encodingValue(String encoding) {
        if (encoding != null) {
            return encoding;
        }
        if (this.defaultEncoding != null) {
            return this.defaultEncoding;
        }
        return null;
    }
    
}
