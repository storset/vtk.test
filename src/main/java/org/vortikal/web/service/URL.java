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
package org.vortikal.web.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.util.web.URLUtil;


/**
 * Class for representing HTTP(s) URLs. Resembles {@link
 * java.net.URL}, except that it has getters/setters for all URL
 * components to achieve easy "on-the-fly" manipulation when
 * constructing URLs.
 *
 * <p>TODO: possibly incorporate authentication information?
 */
public class URL {


    private String protocol = null;
    private String host = null;
    private Integer port = null;
    //private String authority = null;
    //private String userInfo = null;
    private String path = null;
    private String characterEncoding = "utf-8";

    private List<String> parameterNames = new ArrayList<String>();
    private List<String> parameterValues = new ArrayList<String>();

    private String ref = null;
    
    private static final Integer PORT_80 = new Integer(80);
    private static final Integer PORT_443 = new Integer(443);
    
    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";
    

    public URL(String protocol, String host, String path) {
        if (!(PROTOCOL_HTTP.equals(protocol) || PROTOCOL_HTTPS.equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException("Invalid hostname: '" + host + "'");
        }
        if (path == null || "".equals(path.trim()) || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: '" + path + "'");
        }

        this.protocol = protocol.trim();
        this.host = host.trim();
        this.path = path;
    }
    

    public String getProtocol() {
        return this.protocol;
    }
    

    public void setProtocol(String protocol) {
        if (protocol != null) {
            protocol = protocol.trim();
        }
        if (!(PROTOCOL_HTTP.equals(protocol) || PROTOCOL_HTTPS.equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }

        this.protocol = protocol.trim();

        if (PROTOCOL_HTTP.equals(protocol) && PORT_443.equals(this.port)) {
            this.port = PORT_80;
        } else if (PROTOCOL_HTTPS.equals(protocol) && PORT_80.equals(this.port)) {
            this.port = PORT_443;
        }
    }

    
    public String getHost() {
        return this.host;
    }
    

    public void setHost(String host) {
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException(
                "Invalid hostname: '" + host + "'");
        }
        this.host = host;
    }
    

    public Integer getPort() {
        return this.port;
    }
    

    public void setPort(Integer port) {
        if (port != null && port.intValue() <= 0) {
            throw new IllegalArgumentException(
                "Invalid port number: " + port.intValue());
        }
        this.port = port;
    }


    public String getPath() {
        return this.path;
    }
    

    public void setPath(String path) {
        if (path == null || "".equals(path.trim()) || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: '" + path + "'");
        }
        this.path = path;
    }
    
    
    public void addParameter(String name, String value) {
        this.parameterNames.add(name);
        this.parameterValues.add(value);
    }
    

    public void removeParameter(String name) {
        int index = this.parameterNames.indexOf(name);
        while (index != -1) {
            this.parameterNames.remove(index);
            this.parameterValues.remove(index);
            index = this.parameterNames.indexOf(name);
        }
    }
    

    public void clearParameters() {
        this.parameterNames.clear();
        this.parameterValues.clear();
    }
    

    public String getParameter(String parameterName) {
        if (this.parameterNames.contains(parameterName))
            return (String)this.parameterValues.get(this.parameterNames.indexOf(parameterName));
        return null;
    }
    

    public String getRef() {
        return this.ref;
    }
    

    public void setRef(String ref) {
        this.ref = ref;
    }
    

    /**
     * Sets the character encoding used when URL-encoding the path.
     */
    public void setCharacterEncoding(String characterEncoding) {
        if (characterEncoding == null) {
            throw new IllegalArgumentException("Character encoding must be specified");
        }
        java.nio.charset.Charset.forName(characterEncoding);
        this.characterEncoding = characterEncoding;
    }


    public String toString() {
        StringBuffer url = new StringBuffer();
        
        url.append(this.protocol).append("://");
        url.append(this.host);
        if (this.port != null) {
            if (!(this.port.equals(PORT_80) && PROTOCOL_HTTP.equals(this.protocol)
                  || (this.port.equals(PORT_443) && PROTOCOL_HTTPS.equals(this.protocol)))) {
                url.append(":").append(this.port.intValue());
            }
        }
        try {
            url.append(URLUtil.urlEncode(this.path, this.characterEncoding));
        } catch (java.io.UnsupportedEncodingException e) {
            // Ignore, this.characterEncoding is supposed to be valid.
        }
        
        if (!this.parameterNames.isEmpty()) {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            
            for (int i = 0; i < this.parameterNames.size(); i++) {
                parameters.put(this.parameterNames.get(i), this.parameterValues.get(i));
            }

            url.append("?");

            for (Iterator iter = parameters.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                url.append(entry.getKey()).append("=").append(entry.getValue());
                if (iter.hasNext()) url.append("&");
            }
        }

        if (this.ref != null) {
            url.append("#").append(this.ref);
        }

        return url.toString();
    }
    


    /**
     * Utility method to create a URL from a servlet request.
     *
     * @param request the servlet request
     * @return the generated URL
     */
    public static URL create(HttpServletRequest request) {

        String path = request.getRequestURI();
        if (path == null || "".equals(path)) path = "/";

        String host = request.getServerName();
        int port = request.getServerPort();

        URL url = new URL(PROTOCOL_HTTP, host, path);
        url.setPort(new Integer(port));
        if (request.isSecure()) {
            url.setProtocol(PROTOCOL_HTTPS);
        }

        Map<String, String[]> queryStringMap = URLUtil.splitQueryString(request);

        for (String key: queryStringMap.keySet()) {
            String[] values = queryStringMap.get(key);
            for (String value: values) {
                url.addParameter(key, value);
            }
        }
        return url;
    }
    



}
