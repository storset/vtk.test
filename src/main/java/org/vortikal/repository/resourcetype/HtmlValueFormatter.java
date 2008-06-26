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
package org.vortikal.repository.resourcetype;

import java.util.Locale;

import org.vortikal.text.htmlparser.HtmlUtil;

/**
 * This value formatter represents HTML value types. 
 * It supports a single format, namely the <code>escaped</code> format,
 * producing HTML escaping of markup.
 */
public class HtmlValueFormatter implements ValueFormatter {

    private static final String ESCAPED_FORMAT = "escaped";
    private static final String FLATTENED_FORMAT = "flattened";

    private HtmlUtil htmlUtil;
    
    public String valueToString(Value value, String format, Locale locale)
            throws IllegalValueTypeException {
        String html = value.toString();
        if (ESCAPED_FORMAT.equals(format)) {
            return HtmlUtil.escapeHtmlString(html);
        } else if (FLATTENED_FORMAT.equals(format) && this.htmlUtil != null) {
            return this.htmlUtil.flatten(html).toString();
        }
        return html;
    }

    public Value stringToValue(String string, String format, Locale locale) {
        if (ESCAPED_FORMAT.equals(format)) {
            return new Value(HtmlUtil.unescapeHtmlString(string));
        } 
        
        return new Value(string);
    }


    
    public void setHtmlUtil(HtmlUtil htmlUtil) {
        this.htmlUtil = htmlUtil;
    }

}
