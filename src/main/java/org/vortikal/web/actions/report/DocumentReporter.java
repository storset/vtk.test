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
package org.vortikal.web.actions.report;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class DocumentReporter extends AbstractReporter {

    private int pageSize = DEFAULT_SEARCH_LIMIT;
    private Service viewService;
    private int backURL;

    protected abstract Search getSearch(String token, Resource currentResource);

    @Override
    public Map<String, Object> getReportContent(String token, Resource currentResource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        /* Create back to diagram URL. */
        if (backURL > 0) {
            String backURLname;
            if (backURL == 1)
                backURLname = "diagram";
            else
                backURLname = "BACKURL_SET-BUT_UNKNOWN";

            RequestContext requestContext = RequestContext.getRequestContext();
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            Service service = requestContext.getService();

            URL backURL = new URL(service.constructURL(currentResource, securityContext.getPrincipal()));
            backURL.addParameter(REPORT_TYPE_PARAM, backURLname);

            result.put("backURLname", backURLname);
            result.put("backURL", backURL);
        }

        Search search = this.getSearch(token, currentResource);
        if (search == null) {
            return result;
        }

        Position pos = Position.create(request, this.pageSize);
        if (pos.cursor >= Search.MAX_LIMIT) {
            return result;
        }
        search.setCursor(pos.cursor);
        search.setLimit(pageSize);

        ResultSet rs = this.searcher.execute(token, search);
        if (pos.cursor + Math.min(pageSize, rs.getAllResults().size()) >= rs.getTotalHits()) {
            pos.next = null;
        }
        if (pos.cursor + Math.min(pageSize, rs.getAllResults().size()) >= Search.MAX_LIMIT) {
            pos.next = null;
        }

        result.put("result", rs.getAllResults());
        result.put("from", pos.cursor + 1);
        result.put("to", pos.cursor + Math.min(pageSize, rs.getAllResults().size()));
        result.put("total", rs.getTotalHits());
        result.put("next", pos.next);
        result.put("prev", pos.prev);

        boolean[] isReadRestricted = new boolean[rs.getSize()];
        URL[] viewURLs = new URL[rs.getSize()];

        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet p = rs.getResult(i);
            try {
                Resource r = this.repository.retrieve(token, p.getURI(), true);
                isReadRestricted[i] = r.isReadRestricted();
                viewURLs[i] = this.viewService.constructURL(p.getURI());
            } catch (Exception e) {
            }
        }
        result.put("isReadRestricted", isReadRestricted);
        result.put("viewURLs", viewURLs);
        return result;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public void setBackURL(int backURL) {
        this.backURL = backURL;
    }

    private static class Position {
        int cursor = 0;
        int limit = 0;
        URL next = null;
        URL prev = null;

        private Position() {
        }

        static Position create(HttpServletRequest req, int limit) {
            Position position = new Position();
            position.limit = limit;

            int page = 1;
            String pageParam = req.getParameter("page");
            if (pageParam != null) {
                try {
                    page = Integer.parseInt(pageParam.trim());
                } catch (Throwable t) {
                }
            }
            if (page <= 0) {
                page = 1;
            }
            int cursor = (page - 1) * position.limit;
            if (cursor < 0) {
                cursor = 0;
            }
            position.cursor = cursor;
            URL url = URL.create(req);
            position.next = new URL(url).setParameter("page", String.valueOf(page + 1));
            if (page > 1) {
                position.prev = new URL(url).setParameter("page", String.valueOf(page - 1));
            }
            if (page == 2) {
                position.prev.removeParameter("page");
            }
            return position;
        }
    }
}
