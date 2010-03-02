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
package org.vortikal.web.decorating.components;

import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.search.SearchSorting;

public class ImageListingComponent extends ViewRenderingDecoratorComponent {

    private static final int LIMIT = 5;
    private Repository repository;

    private SearchSorting searchSorting;

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String url = request.getStringParameter("uri");
        if (url == null || "".equals(url.trim()) || !isValidPath(url)) {
            return;
        }

        String requestLimit = request.getStringParameter("limit");
        int searchLimit = getSearchLimit(requestLimit);
        
        String type = request.getStringParameter("type");
        if(type != null && type.equals("simple-gallery")) {
            model.put("type", "simple-gallery");
        } else {
            model.put("type", "list");
        }

        String excludeScripts = request.getStringParameter("exclude-scripts");
        if (excludeScripts != null && "true".equalsIgnoreCase(excludeScripts.trim())) {
            model.put("excludeScripts", excludeScripts);
        }

        String token = SecurityContext.getSecurityContext().getToken();
        Resource requestedResource = this.repository.retrieve(token, Path.fromString(url), false);

        if (!requestedResource.isCollection()) {
            return;
        }

        AndQuery mainQuery = new AndQuery();
        mainQuery.add(new UriPrefixQuery(url));
        mainQuery.add(new TypeTermQuery("image", TermOperator.IN));

        Search search = new Search();
        search.setQuery(mainQuery);
        search.setLimit(searchLimit);
        search.setSorting(new SortingImpl(this.searchSorting.getSortFields(requestedResource)));

        ResultSet rs = this.repository.search(token, search);

        model.put("images", rs.getAllResults());
        model.put("folderTitle", requestedResource.getTitle());
        model.put("folderUrl", url);

    }

    private boolean isValidPath(String url) {
        try {
            Path.fromString(url);
            return true;
        } catch (IllegalArgumentException iae) {
        }
        return false;
    }

    private int getSearchLimit(String requestLimit) {
        try {
            int lim = Integer.parseInt(requestLimit);
            if (lim > LIMIT) {
                return lim;
            }
        } catch (NumberFormatException nfe) {
        }
        return LIMIT;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setSearchSorting(SearchSorting searchSorting) {
        this.searchSorting = searchSorting;
    }
}
