/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.display.linkcheck;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;

public class LinkCheckController implements Controller {

    private Repository repository;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();
        Resource resource = this.repository.retrieve(token, resourceURI, false);
        InputStream stream = this.repository.getInputStream(token, resourceURI, false);
        String content = StreamUtil.streamToString(stream, resource.getCharacterEncoding());

        Parser parser = new Parser();
        parser.setInputHTML(content);
        NodeList linkList = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));

        List<String> brokenLinks = new ArrayList<String>();
        for (int i = 0; i < linkList.size(); i++) {
            LinkTag linkTag = (LinkTag) linkList.elementAt(i);
            String link = getLink(linkTag.getLink());
            if (isBroken(resourceURI, link, token)) {
                brokenLinks.add(link);
            }
        }

        PrintWriter writer = response.getWriter();
        for (String brokenLink : brokenLinks) {
            writer.print(brokenLink);
            writer.print("\n");
        }

        return null;
    }

    private String getLink(String link) {
        if (link.startsWith("\\\"")) {
            link = link.substring(2, link.length());
        }
        if (link.endsWith("\\\"")) {
            link = link.substring(0, link.length() - 2);
        }
        return link;
    }

    private String getProcessedLink(Path resourceURI, String link) {
        if (isWebLink(link)) {
            org.vortikal.web.service.URL url = org.vortikal.web.service.URL.parse(link);
            url.clearParameters();
            String host = url.getHost();
            String hostname = this.repository.getId();
            if (hostname.equals(host)) {
                return url.getPathRepresentation();
            }
            return url.toString();
        }
        if (link.contains("?")) {
            link = link.substring(0, link.indexOf("?"));
        }
        if (!link.startsWith("/")) {
            link = resourceURI.getParent().extend(link).toString();
        }
        return link;
    }

    private boolean isBroken(Path resourceURI, String link, String token) {
        // get the processed link to check
        String processedLink = getProcessedLink(resourceURI, link);

        // link to internal resource, check for existence
        if (!isWebLink(processedLink)) {
            return this.isBrokenInternal(processedLink, token);
        }

        // go out on the worldwide web
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(processedLink);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("HEAD");
            // if it doesn't answer within 5sec, mark it as broken
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();
            return urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return true;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private boolean isWebLink(String link) {
        return link.startsWith("http") || link.startsWith("www");
    }

    private boolean isBrokenInternal(String link, String token) {
        try {
            Path path = Path.fromString(link);
            boolean exists = this.repository.exists(token, path);
            return !exists;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (AuthorizationException e) {
            return true;
        } catch (AuthenticationException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
