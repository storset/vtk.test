/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.text.htmlparser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlText;


public class HtmlUtil {

    private HtmlPageParser htmlParser;
    private Map<String, String> htmlEntityMap = new HashMap<String, String>();

    public HtmlPage parse(InputStream in, String encoding) throws Exception {
        return this.htmlParser.parse(in, encoding);
    }


    public String flatten(String html) {
        try {
            StringBuilder sb = new StringBuilder();
            HtmlFragment fragment = this.htmlParser.parseFragment(html);
            for (HtmlContent c : fragment.getContent()) {
                sb.append(flatten(c));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    
    public String flatten(HtmlContent content) {
        StringBuilder sb = new StringBuilder();
        if (content instanceof HtmlElement) {
            HtmlElement htmlElement = (HtmlElement) content;
            for (HtmlContent child : htmlElement.getChildNodes()) {
                sb.append(flatten(child));
            }
        } else if (content instanceof HtmlText) {
            String str = content.getContent();
            sb.append(processHtmlEntities(str));
        }
        return sb.toString();
    }
    
    
    public static String escapeHtmlString(String html) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            switch (c) {
            case '&':
                result.append("&amp;");
                break;
            case '"':
                result.append("&quot;");
                break;
            case '\'':
                result.append("&apos;");
                break;
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            default:
                result.append(c);
                break;
            }
        }
        return result.toString();
    }
    
    
    public static String unescapeHtmlString(String html) {
        html = html.replaceAll("&amp;", "&");
        html = html.replaceAll("&quot;", "\"");
        html = html.replaceAll("&apos;", "'");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        return html;
    }
    
    
    private StringBuilder processHtmlEntities(String content) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '&') {
                int j = i + 1;

                String entity = null;
                while (j < content.length()) {
                    boolean validChar = validEntityCharAtIndex(content, j);
                    if (!validChar && content.charAt(j) == ';' && j > i + 1) {
                        entity = content.substring(i + 1, j);
                        i = j;
                        break;
                    } else if (!validChar) {
                        break;
                    }
                    j++;
                }
                if (entity != null) {
                    if (this.htmlEntityMap.containsKey(entity)) {
                        result.append(this.htmlEntityMap.get(entity));
                    } else {
                        result.append("&").append(entity).append(";");
                    }
                } 
            } else {
                result.append(ch);
            }
        }
        return result;
    }

    
    private boolean validEntityCharAtIndex(String content, int i) {
        if (i >= content.length()) return false;
        char c = content.charAt(i);
        return i < content.length() && c != ';' 
            && (('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c));
    }

    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    public void setHtmlEntityMap(Map<String, String> htmlEntityMap) {
        this.htmlEntityMap = htmlEntityMap;
    }

}
