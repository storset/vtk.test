/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.web.view.decorating;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;

import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.text.HtmlUtil;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.servlet.BufferedResponseWrapper;
import org.vortikal.web.servlet.ConfigurableRequestWrapper;

/**
 * This view wrapper takes a view and decorates the output from that
 * view. A list of (textual) {@link Decorator decorators} is matched. 
 * If any matches, a buffered servlet response is created before the view is rendered, 
 * and the matching decorators applied. 
 * 
 * <p>Main application areas include merging common components
 * like menus, breadcrumb trails, etc. into the HTML generated by
 * views and substituting content (like SSI directives) in the response.
 * 
 * <p>Subclasses that wish to perform custom filtering (aside from
 * what is possible using the {@link Decoraator} interface) may
 * override {@link #preRender preRender()} or {@link #postRender
 * postRender()} methods, which provide access to the buffered
 * response.
 *
 * <p>The Content-Type header written to the response is the same as
 * that of the original response, with a possible modification of the
 * <code>charset</code> parameter (see below).
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>decorators</code> - an array of {@link
 *   Decorator decorators} to apply to the textual content
 *   that was the result of the wrapped view invocation.
 *   <li><code>guessCharacterEncodingFromContent</code> (boolean) -
 *   whether to check (HTML) body contents for character encoding, if
 *   it was not specified by the <code>Content-Type</code> header of
 *   the wrapped view. Default is <code>false</code>.
 *   <li><code>appendCharacterEncodingToContentType</code> (boolean)
 *   - if set to <code>false</code>, no <code>charset</code> parameter
 *   will be added to the <code>Content-Type</code> header set by this
 *   view. Default is <code>true</code>.
 *   <li><code>forcedOutputEncoding</code> - if this option is set,
 *   the output will be written using that character encoding.
 * </ul>
 * 
 * @see Decorator
 * @see Decorator#match(HttpServletRequest)
 */
public class DecoratingViewWrapper implements ViewWrapper, ReferenceDataProviding {

    protected Log logger = LogFactory.getLog(this.getClass());

    private Decorator[] decorators;
    private ReferenceDataProvider[] referenceDataProviders;
    private String forcedOutputEncoding;
    private boolean guessCharacterEncodingFromContent = false;
    private boolean appendCharacterEncodingToContentType = true;
    private Map staticHeaders = null;

    public void setDecorators(Decorator[] decorators) {
        this.decorators = decorators;
    }


    public void setForcedOutputEncoding(String forcedOutputEncoding) {
        this.forcedOutputEncoding = forcedOutputEncoding;
    }


    public void setGuessCharacterEncodingFromContent(
            boolean guessCharacterEncodingFromContent) {
        this.guessCharacterEncodingFromContent = guessCharacterEncodingFromContent;
    }


    public void setAppendCharacterEncodingToContentType(
            boolean appendCharacterEncodingToContentType) {
        this.appendCharacterEncodingToContentType = appendCharacterEncodingToContentType;
    }


    public ReferenceDataProvider[] getReferenceDataProviders() {
        return this.referenceDataProviders;
    }


    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }


    public void setStaticHeaders(Map staticHeaders) {
        this.staticHeaders = staticHeaders;
    }
    

    public void renderView(View view, Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
        List decoratorList = new ArrayList();
        
        if (this.decorators != null) {
            for (int i = 0; i < this.decorators.length; i++) {
                if (this.decorators[i].match(request)) {
                    decoratorList.add(this.decorators[i]);
                }
            }        
        }

        if (decoratorList.size() == 0) {
            view.render(model, request, response);
            return;
        } 

        ConfigurableRequestWrapper requestWrapper = new ConfigurableRequestWrapper(request);
        requestWrapper.setMethod("GET");
        BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(response);
        
        view.render(model, requestWrapper, responseWrapper);
            
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("About to post process buffered content, content type: "
                    + responseWrapper.getContentType()
                    + ", character encoding: "
                    + responseWrapper.getCharacterEncoding());
        }
        decorate(model, request, decoratorList, responseWrapper);
    }


    private void decorate(Map model, HttpServletRequest request,
                           List decoratorList, BufferedResponseWrapper bufferedResponse)
        throws Exception {

        byte[] contentBuffer = bufferedResponse.getContentBuffer();

        String contentType = bufferedResponse.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        contentType = contentType.trim();
        
        String characterEncoding = null;
        
        
        if (contentType.indexOf("charset") != -1
                && contentType.indexOf(";") != -1) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
            characterEncoding = bufferedResponse.getCharacterEncoding();
        } else if (this.guessCharacterEncodingFromContent) {
            characterEncoding = HtmlUtil.getCharacterEncodingFromBody(contentBuffer);
        }

        if (characterEncoding == null) {
            characterEncoding = bufferedResponse.getCharacterEncoding();
        }

        if (!Charset.isSupported(characterEncoding)) {
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Unable to perform content filtering on response  "
                        + bufferedResponse + " for requested URL "
                        + request.getRequestURL() + ": character encoding '"
                        + characterEncoding
                        + "' is not supported on this system");
            }
            writeResponse(bufferedResponse);
            return;
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Reading buffered content using character encoding "
                    + characterEncoding);
        }


        Content content = new ContentImpl(new String(contentBuffer, characterEncoding),
                                          characterEncoding);

        if (this.decorators != null) {
            for (Iterator iter = decoratorList.iterator(); iter.hasNext();) {
                Decorator decorator = (Decorator) iter.next();
                
                decorator.decorate(model, request, content);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Ran content filter " + decorator);
                }
            }
        }

        if (this.forcedOutputEncoding != null) {
            characterEncoding = this.forcedOutputEncoding;
        }

        if (this.appendCharacterEncodingToContentType
                && ContentTypeHelper.isTextContentType(contentType)) {

            contentType = contentType + ";charset=" + characterEncoding;
        }

        writeResponse(content.getContent().getBytes(characterEncoding), bufferedResponse,
                contentType);
    }


    /**
     * Writes the buffer from the wrapped response to the actual
     * response. Sets the HTTP header <code>Content-Length</code> to
     * the size of the buffer in the wrapped response.
     * 
     * @param responseWrapper the wrapped response.
     * @exception Exception if an error occurs.
     */
    protected void writeResponse(BufferedResponseWrapper responseWrapper)
            throws Exception {
        HttpServletResponse response = responseWrapper.getHttpServletResponse();
        writeStaticHeaders(response);

        byte[] content = responseWrapper.getContentBuffer();
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Write response: Content-Length: " + content.length
                    + ", unspecified content type");
        }
        response.setContentLength(content.length);
        ServletOutputStream outStream = response.getOutputStream();
        outStream.write(content);
        outStream.flush();
        outStream.close();
    }


    protected void writeResponse(byte[] content,
            BufferedResponseWrapper responseWrapper, String contentType)
            throws Exception {
        HttpServletResponse response = responseWrapper.getHttpServletResponse();

        writeStaticHeaders(response);
        ServletOutputStream outStream = response.getOutputStream();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Write response: Content-Length: " + content.length
                    + ", Content-Type: " + contentType);
        }
        response.setContentType(contentType);
        response.setContentLength(content.length);
        outStream.write(content);
        outStream.flush();
        outStream.close();
    }


    protected void writeStaticHeaders(HttpServletResponse response) throws Exception {
        if (this.staticHeaders == null) {
            return;
        }

        for (Iterator i = this.staticHeaders.entrySet().iterator(); i.hasNext();) {
            
            Map.Entry entry = (Map.Entry) i.next();
            if (entry.getValue() instanceof Date) {
                response.setDateHeader((String) entry.getKey(), ((Date) entry.getValue()).getTime());
            } else {
                response.setHeader((String) entry.getKey(), ((String) entry.getValue()));
            }
        }
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":");
        sb.append(" [decorators = ").append(
            (this.decorators != null) ? Arrays.asList(this.decorators) : null);
        sb.append("]");
        return sb.toString();
    }

}
