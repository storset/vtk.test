/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.repository.systemjob;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.Parser;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.security.SecurityContext;

/**
 *
 */
public class IndexQueryPathSelector implements PathSelector {

    private static Log logger = LogFactory.getLog(IndexQueryPathSelector.class);
    
    protected Searcher searcher;
    protected Parser parser;
    protected ResourceTypeTree resourceTypeTree;
    
    private String queryString;
    private String sortString;
    private boolean onlyPublishedResources = false;
    
    private int limit = 2000;

    private static final PropertySelect NO_PROPERTIES = new PropertySelect() {
        @Override
        public boolean isIncludedProperty(PropertyTypeDefinition propertyDefinition) {
            return false;
        }
    };
    
    @Override
    public void selectWithCallback(Repository repository,
                                   SystemChangeContext context,
                                   PathSelectCallback callback) throws Exception{

        final String token = SecurityContext.exists() ? SecurityContext.getSecurityContext().getToken() : null;
                
        Query query = getQuery(context);
        Sorting sort = getSorting(context);
        Search search = new Search();
        search.setQuery(query);
        search.setSorting(sort);
        search.setLimit(this.limit);
        search.setOnlyPublishedResources(this.onlyPublishedResources);
        search.setPropertySelect(NO_PROPERTIES);
        ResultSet results = this.searcher.execute(token, search);

        if (logger.isDebugEnabled()) {
            logger.debug("Ran query " + query + (sort != null ? ", sorting: " + sort + "," : "")
                    + " with " + results.getSize()
                    + " results of total " + results.getTotalHits());
        }

        callback.beginBatch(results.getSize());
        for (PropertySet result: results) {
            callback.select(result.getURI());
        }
    }
    
    protected Query getQuery(SystemChangeContext context) {
        if (this.queryString == null) {
            throw new IllegalStateException("No query string configured");
        }
        return this.parser.parse(this.queryString);
    }
    
    protected Sorting getSorting(SystemChangeContext context) {
        return this.parser.parseSortString(this.sortString);
    }
    
    protected Query getSystemJobQuery(SystemChangeContext context) {
        OrQuery orQuery = new OrQuery();

        // XXX Logic as query string:
        //     system-job-status@JOBID !exists OR  system-job-status@JOBID < {$currentTime}
        //
        // .. which matches everything ..
        // What's the intention here ? Looks like it can be removed entirely.
        
        PropertyExistsQuery systemJobPropertyExistsQuery = new PropertyExistsQuery(context.getSystemJobStatusPropDef(), true);
        systemJobPropertyExistsQuery.setComplexValueAttributeSpecifier(context.getJobName());
        orQuery.add(systemJobPropertyExistsQuery);

        PropertyTermQuery systemJobPropertyQuery = new PropertyTermQuery(context.getSystemJobStatusPropDef(), 
                context.getTime(),
                TermOperator.LT);
        systemJobPropertyQuery.setComplexValueAttributeSpecifier(context.getJobName());
        orQuery.add(systemJobPropertyQuery);

        return orQuery;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    
    public void setSortString(String sortString) {
        this.sortString = sortString;
    }
    
    public void setParser(Parser parser) {
        this.parser = parser;
    }
    
    public boolean isOnlyPublishedResources() {
        return this.onlyPublishedResources;
    }
    
    public void setLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be >= 1");
        }
        this.limit = limit;
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }
    
    public void setOnlyPublishedResources(boolean onlyPublished) {
        this.onlyPublishedResources = onlyPublished;
    }

}
