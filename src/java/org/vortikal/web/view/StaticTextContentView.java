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

import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;


/**
 * View implementation that displays static, preconfigured text
 * content.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>content</code> - the content to display
 *   <li><code>contentType</code> - the content type written to the
 *       response
 *   <li><code>characterEncoding</code> - the character encoding
 *       written to the response
 * </ul>
 * 
 * <p>Sets the following HTTP headers:
 * <ul>
 *   <li><code>Content-Type</code> - the values of
 *       <code>contentType</code> and <code>characterEncoding</code>
 *   <li><code>Content-Length</code> - the length of the content
 * </ul>
 *
 */
public class StaticTextContentView implements InitializingBean, View {

    private String content = null;
    private String contentType = null;
    private String characterEncoding = null;
    private byte[] buffer;
    
    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.content == null) {
            throw new BeanInitializationException(
                "Bean property 'content' must be set");
        }
        if (this.contentType == null) {
            throw new BeanInitializationException(
                "Bean property 'contentType' must be set");
        }
        if (this.characterEncoding == null) {
            throw new BeanInitializationException(
                "Bean property 'characterEncoding' must be set");
        }

        this.buffer = this.content.getBytes(this.characterEncoding);
    }



    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        response.setHeader("Content-Type", this.contentType
                           + ";charset=" + this.characterEncoding);

        response.setIntHeader("Content-Length", this.buffer.length);
        response.setStatus(HttpServletResponse.SC_OK);
  
        OutputStream out = null;
        try {
            out = response.getOutputStream();
                out.write(buffer);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
    


}
