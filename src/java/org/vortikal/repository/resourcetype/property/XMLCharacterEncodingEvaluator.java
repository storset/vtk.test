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
package org.vortikal.repository.resourcetype.property;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.security.Principal;
import org.vortikal.util.io.StreamUtil;



/**
 * Evaluator for XML character encoding.
 *
 * <p>
 * XXX: This class should ideally use some XML representation class,
 * such as JDOM Document. However, org.jdom.Documents do not return
 * XML declarations as processing instructions, and there is no other
 * way to obtain the character encoding using JDOM, so we are forced
 * to inspect the raw content.
 *
 * <p>
 * XXX: Could possibly use org.dom4j.Document#getXMLEncoding ?
 * 
 */
public class XMLCharacterEncodingEvaluator implements CreatePropertyEvaluator,
        ContentModificationPropertyEvaluator {

    private int maxBytes = 10000000;

    private static Pattern CHARSET_PATTERN = Pattern.compile(
        "^\\s*<\\?xml\\s+[^>]*\\s+encoding=[\"']"
        + "([A-Za-z0-9._\\-]+)[\"'][^>]*\\?>.*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    private Log logger = LogFactory.getLog(this.getClass());

    private String defaultEncoding = null;

    public void setDefaultEncoding(String defaultEncoding)
        throws IllegalCharsetNameException, UnsupportedCharsetException {
        Charset.forName(defaultEncoding);
        this.defaultEncoding = defaultEncoding;
    }
    

    public void setMaxBytes(int maxBytes) {
        this.maxBytes = maxBytes;
    }
    

    public boolean create(Principal principal, Property property, 
            PropertySet ancestorPropertySet, boolean isCollection, Date time)
    throws PropertyEvaluationException {
        if (this.defaultEncoding != null) {
            property.setStringValue(this.defaultEncoding);
            return true;
        }
        return false;
    }
    
    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {

        String characterEncoding = null;
        String xmlContent = null;
      
        try {
            InputStream inputStream = content.getContentInputStream();
            byte[] buffer = StreamUtil.readInputStream(inputStream, this.maxBytes);
            xmlContent = new String(buffer, "utf-8");

        } catch (Exception e) {
            throw new PropertyEvaluationException("Unable to get Stringcontent representation", e);
        }

        if (xmlContent == null) {
            throw new PropertyEvaluationException("Unable to get Stringcontent representation");
        }

        Matcher m = CHARSET_PATTERN.matcher(xmlContent);

        if (m.matches()) {
            if (logger.isDebugEnabled())
                logger.debug("Regexp match in XML declaration for pattern "
                             + CHARSET_PATTERN.pattern());
            characterEncoding = m.group(1);
            
            try {
                Charset.forName(characterEncoding);
            } catch (Exception e) {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Invalid character encoding '" + characterEncoding
                        + "' for XML document <string>, using default encoding");
                characterEncoding = null;
            }
        }
            
        if (characterEncoding == null && this.defaultEncoding != null) {
            characterEncoding = this.defaultEncoding;
        }

        if (characterEncoding == null) {
            return false;
        }

        property.setStringValue(characterEncoding);
        return true;
    }
    
}
