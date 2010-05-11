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

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class LastModifiedReporter extends AbstractReporter {

    private static final int LIMIT = 100;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition sortPropDef;
    private SortFieldDirection sortOrder;
    private Service viewService;

    public Map<String, Object> getReportContent(String token, Resource currentResource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();

        AndQuery query = new AndQuery();
        query.add(new TypeTermQuery("file", TermOperator.IN));
        query.add(new UriPrefixQuery(currentResource.getURI().toString()));

        Search search = new Search();
        search.setLimit(LIMIT);
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.sortPropDef, this.sortOrder));
        search.setSorting(sorting);
        search.setQuery(query);

        ResultSet rs = this.searcher.execute(token, search);
        result.put("lastModifiedList", rs.getAllResults());

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

    @Required
    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }

    @Required
    public void setSortOrder(SortFieldDirection sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    public PropertyTypeDefinition getTitlePropDef() {
        return titlePropDef;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
}