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
package org.vortikal.web.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Content filter that merges the supplied text content into the
 * <code>&lt;body&gt;</code> element of the original content (if there
 * is one). The text is placed at the beginning of the body content.
 *
 * <p>This type of filter may for example be used to provide a menu
 * component on all HTML pages.
 */
public class HtmlHeaderContentFilter
  extends AbstractViewProcessingTextContentFilter {


    private static Pattern HEADER_REGEXP =
        Pattern.compile("<\\s*body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);



    protected String processInternal(String content, String header)
        throws Exception {

        Matcher headerMatcher = HEADER_REGEXP.matcher(content);

        if (headerMatcher.find()) {
            if (debug && logger.isDebugEnabled()) {
                logger.debug("Found <body> or similar, will add header");
            }
            int index = headerMatcher.start(1);
            return content.substring(0, index) + header + content.substring(index);
        } 

        if (debug && logger.isDebugEnabled()) {
            logger.debug("Did not find <body> or similar, returning original content");
        }
        return content;
    }

}
