/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.index.LuceneIndexManager;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.search.query.DumpQueryTreeVisitor;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryBuilderFactory;
import org.vortikal.repository.search.query.SortBuilder;
import org.vortikal.repository.search.query.SortBuilderImpl;
import org.vortikal.repository.search.query.security.LuceneResultSecurityInfo;
import org.vortikal.repository.search.query.security.QueryResultAuthorizationManager;
import org.vortikal.repository.search.query.security.ResultSecurityInfo;

/**
 * Implementation of repository {@link org.vortikal.repository.search.Searcher}
 * using Lucene.
 * 
 *  TODO: Integrate ACL filtering in search, don't do it post-search, 
 *        index relevant parts of ACL lists.
 *        
 *  <p>Configurable bean properties:</p>
 *  <ul>
 *      <li><code>maxAllowedHitsPerQuery</code>
 *      The internal maximum number of hits allowed for any
 *      query <em>before</em> any post-processing of the results.
 *      This is a low-level hard system limit, and no query result will ever be 
 *      bigger than this value.
 *      </li>
 *      
 *      <li><code>indexAccessor</code>
 *      The low-level Lucene index accessor instance that this searcher should use</li>
 *      
 *      <li><code>queryResultAuthorizationManager</code>
 *      Manager used to authorize results in queries based on the security token
 *      of the client performing the query. Security filtering will not be 
 *      applied to results if this bean is not configured.</li>
 *      
 *      <li><code>queryBuilderFactory</code>
 *      Factory for building Lucene queries (required)</li>
 *  </ul>
 * @author oyviste
 *
 */
public class OldSearcherImpl implements Searcher, InitializingBean {

    private final Log logger = LogFactory.getLog(OldSearcherImpl.class);

    private LuceneIndexManager indexAccessor;
    private DocumentMapper documentMapper;
    private QueryResultAuthorizationManager queryResultAuthorizationManager;
    private QueryBuilderFactory queryBuilderFactory;
    
    private SortBuilder sortBuilder = new SortBuilderImpl();
    
    private FieldValueMapper fieldValueMapper;
    
    /**
     * The maximum number of hits allowed for any
     * query <em>before</em> processing of the results by layers above Lucene.
     * This limit includes unauthorized hits that are not supplied to client.
     * 
     */
    private int maxAllowedHitsPerQuery = 50000;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.maxAllowedHitsPerQuery <= 0) {
            throw new BeanInitializationException("Property 'maxAllowedHitsPerQuery'" 
                                         + " must be an integer greater than zero.");
        }
        
        if (this.queryResultAuthorizationManager == null) {
            this.logger.warn("No authorization manager configured, "
                             + "queries will not be filtered with regard to"
                             + " permissions.");
        }
    }
    
    public ResultSet execute(String token, Search search)
        throws QueryException {

        Query query = search.getQuery();
        Sorting sorting = search.getSorting();
        
        int limit = search.getLimit();
        
        int cursor = search.getCursor();
        PropertySelect selectedProperties = search.getPropertySelect();
        
        if (selectedProperties == null) {
            throw new IllegalArgumentException("Argument selectedProperties cannot be NULL");
        }


        org.apache.lucene.search.Query q =
            this.queryBuilderFactory.getBuilder(query).buildQuery();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Built lucene query '" + q 
                                            + "' from query " 
                                            + query.accept(new DumpQueryTreeVisitor(), null));
        }

        IndexSearcher searcher = null;
        try {
            searcher = this.indexAccessor.getIndexSearcher();
            
            // The n parameter is the upper maximum number of results we allow
            // Lucene to fetch.
            // int n = cursor + maxResults;
            // if (n > this.maxAllowedHitsPerQuery) n = this.maxAllowedHitsPerQuery;
            
            long start = System.currentTimeMillis();
            TopDocs topDocs = null;
            if (sorting != null) {
                // XXX: Sorting performance with many hits may very well suck ..
                Sort sort = this.sortBuilder.buildSort(sorting);
                topDocs = searcher.search(q, null, 
                                        this.maxAllowedHitsPerQuery, sort);
            } else {
                topDocs = searcher.search(q, null, 
                                        this.maxAllowedHitsPerQuery);
            }
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Got " + topDocs.totalHits 
                                        + " total hits for Lucene query: " + q);
                
                if (sorting != null) {
                    this.logger.debug("Sorted Lucene query took: " 
                            + (System.currentTimeMillis()-start) + " ms");
                } else {
                    this.logger.debug("Unsorted Lucene query took: " 
                            + (System.currentTimeMillis()-start) + " ms");
                }
            }
            
            ResultSet rs = buildResultSet(topDocs.scoreDocs, topDocs.totalHits, 
                                          searcher.getIndexReader(), token, limit,
                                          cursor, selectedProperties);
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Total query time including result set building: " 
                        + (System.currentTimeMillis()-start) + " ms");
            }
            
            return rs;
            
        } catch (IOException io) {
            this.logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                this.indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {
                this.logger.warn("IOException while releasing index searcher", io);
            }
        }
    }
    
    private ResultSetImpl buildResultSet(ScoreDoc[] docs, int totalHits,
                                        IndexReader reader, String token, int limit,
                                        int cursor, PropertySelect selectedProps) 
        throws IOException {
        
        ResultSetImpl resultSet = new ResultSetImpl(limit);
        resultSet.setTotalHits(totalHits); // XXX: Revealing total hits includes unauthorized hits.
        
        // Get list of all authorized hits.
        // XXX: Memory requirements for queries with many matches ..
        List<Document> results = getAuthorizedResults(docs, reader, 
                                                        token, selectedProps);
        
        int nResults = results.size();
        if (cursor < nResults) {
            int end = cursor + limit;
            if (end > nResults) end = nResults;
            
            for (Document doc: results.subList(cursor, end)) {
                resultSet.addResult(this.documentMapper.getPropertySet(doc));
            }
        }
        
        return resultSet;
    }
    
    private List<Document> getAuthorizedResults(ScoreDoc[] docs, 
                          IndexReader reader, String token, PropertySelect select) 
        throws IOException {
        
        FieldSelector fieldSelector
            = this.documentMapper.getDocumentFieldSelector(select);

        List<Document> results = new ArrayList<Document>(docs.length);
        
        if (this.queryResultAuthorizationManager != null) {
            
            List<ResultSecurityInfo> rsiList = 
                new ArrayList<ResultSecurityInfo>(docs.length);
            
            for (ScoreDoc scoreDoc: docs) {
                Document doc = reader.document(scoreDoc.doc, fieldSelector);
                rsiList.add(new LuceneResultSecurityInfo(doc, this.fieldValueMapper));
            }
            
            this.queryResultAuthorizationManager.authorizeQueryResults(token, rsiList);
            
            for (ResultSecurityInfo rsi: rsiList) {
                if (rsi.isAuthorized()) {
                    results.add(((LuceneResultSecurityInfo)rsi).getDocument());
                }
            }
        } else {
            for (ScoreDoc scoreDoc: docs) {
                results.add(reader.document(scoreDoc.doc, fieldSelector));
            }
        }
        
        return results;
    }

//    private ResultSetImpl buildResultSetOld(ScoreDoc[] docs, int totalHits,
//                                         IndexReader reader, String token, int maxResults,
//                                         int cursor, PropertySelect selectedProperties)
//        throws IOException {
//
//        ResultSetImpl rs = new ResultSetImpl(docs.length);
//        rs.setTotalHits(totalHits);
//
//        if (docs.length == 0 || cursor >= docs.length) {
//            return rs; // Empty result set
//        }
//
//        if (maxResults < 0)
//            maxResults = 0;
//
//        int end = (cursor + maxResults) < docs.length ? 
//                                              cursor + maxResults : docs.length;
//
//        FieldSelector selector = 
//            this.documentMapper.getDocumentFieldSelector(selectedProperties);
//        
//        if (this.queryResultAuthorizationManager != null) {
//            // XXX: cursor/maxresults might be confusing after filtering, define it 
//            //      properly and fix this.
//            List<ResultSecurityInfo> rsiList = 
//                        new ArrayList<ResultSecurityInfo>(end-cursor);
//            for (int i = cursor; i < end; i++) {
//                Document doc = reader.document(docs[i].doc, selector);
//                rsiList.add(new LuceneResultSecurityInfo(doc));
//            }
//            
//            long start = System.currentTimeMillis();
//            this.queryResultAuthorizationManager.authorizeQueryResults(token, rsiList);
//            long finish = System.currentTimeMillis();
//            
//            if (this.logger.isDebugEnabled()) {
//                this.logger.debug("Query result authorization took " 
//                        + (finish-start) + " ms");
//            }
//            
//            // XXX: fix cursor crap, give client the expected number of results, even
//            // after security filtering, figure out best solution (use dummy results ?)
//            // Add only authorized docs to ResultSet
//            // XXX: add metadata to resultset about how many un-authorized hits, etc.
//            for (ResultSecurityInfo info: rsiList) {
//                if (info.isAuthorized()) {
//                    rs.addResult(this.documentMapper.getPropertySet(
//                            ((LuceneResultSecurityInfo)info).getDocument()));
//                }
//            }
//            
//        } else {
//            for (int i = cursor; i < end; i++) {
//                Document doc = reader.document(docs[i].doc, selector);
//                rs.addResult(this.documentMapper.getPropertySet(doc));
//            }
//        }
//        
//        return rs;
//    }
    
    @Required
    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Required
    public void setIndexAccessor(LuceneIndexManager indexAccessor) {
        this.indexAccessor = indexAccessor;
    }

    @Required
    public void setQueryBuilderFactory(QueryBuilderFactory queryBuilderFactory) {
        this.queryBuilderFactory = queryBuilderFactory;
    }
    
    public void setMaxAllowedHitsPerQuery(int maxAllowedHitsPerQuery) {
        this.maxAllowedHitsPerQuery = maxAllowedHitsPerQuery;
    }

    public int getMaxAllowedHitsPerQuery() {
        return this.maxAllowedHitsPerQuery;
    }

    public void setQueryResultAuthorizationManager(QueryResultAuthorizationManager
                                                   queryResultAuthorizationManager) {
        this.queryResultAuthorizationManager = queryResultAuthorizationManager;
    }
    
    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }

}
