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
package org.vortikal.repositoryimpl.search.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.QueryFilter;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriOperator;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.repositoryimpl.index.mapping.FieldNameMapping;
import org.vortikal.repositoryimpl.search.query.InversionFilter;
import org.vortikal.repositoryimpl.search.query.QueryBuilder;
import org.vortikal.repositoryimpl.search.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 *
 */
public class UriTermQueryBuilder implements QueryBuilder {

    private UriTermQuery query;
    public UriTermQueryBuilder(UriTermQuery query) {
        this.query = query;
    }
    
    public org.apache.lucene.search.Query buildQuery() throws QueryBuilderException {
        String uri = this.query.getUri();
        
        UriOperator operator = this.query.getOperator();

        if (operator == UriOperator.EQ) {
            // URI equality
            return new TermQuery(new Term(FieldNameMapping.URI_FIELD_NAME, uri));
        } 

        if (operator == UriOperator.NE) {
            // URI NOT equal
            TermQuery tq = 
                new TermQuery(new Term(FieldNameMapping.URI_FIELD_NAME, uri));
            return new ConstantScoreQuery(new InversionFilter(new QueryFilter(tq)));
            //            throw new QueryBuilderException("Term operator 'NE' not yet supported.");
        } 

        throw new QueryBuilderException("Operator '" + operator + "' not legal for uri queries.");
    }

}
