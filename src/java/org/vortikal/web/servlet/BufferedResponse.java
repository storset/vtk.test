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
package org.vortikal.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.util.web.HttpUtil;



/**
 * A HttpServletResponse implementation that buffers the content and
 * headers.
 *
 */
public class BufferedResponse implements HttpServletResponse {

    private int status = 200;
    private String statusMessage = "OK";
    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    // FIXME: allow several headers having the same name?
    private Map headers = new HashMap(); 
    private List cookies = new ArrayList();
    private int bufferSize = 1000;
    private String contentType = null;
    private int contentLength = 0;
    private Locale locale = Locale.getDefault();
    private String characterEncoding = null;
    private boolean committed = false;
    

    public String getContentType() {
        if (this.characterEncoding != null) {
            return this.contentType + ";charset=" + this.characterEncoding;
        }
        return this.contentType;
    }
    
    public byte[] getContentBuffer() {
        return this.bufferStream.toByteArray();
    }

    public int getStatus() {
        return this.status;
    }
    
    public int getContentLength() {
        return this.bufferStream.size();
    }
    

    /* HttpServletResponse implementation */

    public String getCharacterEncoding() {
        if (this.characterEncoding == null) {
            return "iso-8859-1";
        }
        return this.characterEncoding;
    }

    public PrintWriter getWriter() throws IOException {
        return new ByteArrayServletOutputStreamWriter(new ByteArrayServletOutputStream(
                                            bufferStream, this.getCharacterEncoding()));
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void resetBuffer() {
        this.bufferStream.reset();
    }
    
    public boolean isCommitted() {
        return this.committed;
    }

    public void reset() {
        this.bufferStream.reset();
    }

    public void flushBuffer() throws IOException {
        this.committed = true;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        ServletOutputStream s = new ByteArrayServletOutputStream(this.bufferStream,
                                                this.characterEncoding);
        return s;
    }

    public void setContentType(String contentType) {
        processContentTypeHeader(contentType);
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public boolean containsHeader(String header) {
        return this.headers.containsKey(header);
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeUrl(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public void sendError(int status, String statusMessage) throws IOException {
        this.status = status;
        this.statusMessage = statusMessage;
        this.committed = true;
    }

    public void sendError(int status) throws IOException {
        this.status = status;
        this.statusMessage = HttpUtil.getStatusMessage(status);
        this.committed = true;
    }

    public void sendRedirect(String url) throws IOException {
        this.status = HttpServletResponse.SC_MOVED_TEMPORARILY;
        this.statusMessage = "Moved Temporarily";
        this.headers.put("Location", url);
        this.committed = true;
    }

    public void setDateHeader(String header, long date) {
        this.headers.put(header, new Date(date));
    }

    public void addDateHeader(String header, long date) {
        this.headers.put(header, new Date(date));
    }

    public void setHeader(String header, String value) {
        header = header.trim();
        value = value.trim();
        applyHeaderSideEffects(header, value);
        this.headers.put(header, value);
    }

    public void addHeader(String header, String value) {
        header = header.trim();
        applyHeaderSideEffects(header, value);
        this.headers.put(header, value);
    }

    public void setIntHeader(String header, int value) {
        header = header.trim();
        applyHeaderSideEffects(header, String.valueOf(value));
        this.headers.put(header, new Integer(value));
    }

    public void addIntHeader(String header, int value) {
        header = header.trim();
        applyHeaderSideEffects(header, String.valueOf(value));
        this.headers.put(header, new Integer(value));
    }

    public void setStatus(int status) {
        this.status = status;
        this.statusMessage = HttpUtil.getStatusMessage(status);
    }

    public void setStatus(int status, String statusMessage) {
        this.status = status;
        this.statusMessage = statusMessage;
    }

    private void applyHeaderSideEffects(String header, String value) {

        if ("Content-Type".equals(header)) {

            processContentTypeHeader(value);

        } else if ("Content-Length".equals(header)) {
            try {
                this.contentLength = Integer.parseInt(value);
            } catch (Exception e) {
                
            }
        }
    }


    private void processContentTypeHeader(String value) {
        if (value.matches(".+/.+;.*charset.*=.+")) {

            String contentType = value.substring(
                0, value.indexOf(";")).trim();
            String characterEncoding = value.substring(
                value.indexOf("=") + 1).trim();

            if (characterEncoding.startsWith("\"")
                && characterEncoding.endsWith("\"")) {

                characterEncoding = characterEncoding.substring(
                    1, characterEncoding.length() - 1).trim();
            }
            this.contentType = contentType;
            this.characterEncoding = characterEncoding;

        } else {
            this.contentType = value;
        }
    }
    
}
