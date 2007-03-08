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
package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.query.NameTermQuery;
import org.vortikal.repository.query.TermOperator;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 *
 */
public class NameTermQueryBuilder implements QueryBuilder {

    NameTermQuery ntq;
    
    public NameTermQueryBuilder(NameTermQuery q) {
        this.ntq = q;
    }
    
    public org.apache.lucene.search.Query buildQuery() {
        String term = this.ntq.getTerm();
        TermOperator op = this.ntq.getOperator();
        
        if (op == TermOperator.EQ) {
            TermQuery tq = 
                new TermQuery(new Term(DocumentMapper.NAME_FIELD_NAME, term));
            
            return tq;
        }
        
        boolean includeLower = false;
        boolean includeUpper = false;
        String upperTerm = null;
        String lowerTerm = null;
        
        if (op == TermOperator.GE) {
            lowerTerm = term;
            includeLower = true;
            includeUpper = true;
        } else if (op == TermOperator.GT) {
            lowerTerm = term;
            includeUpper = true;
        } else if (op == TermOperator.LE) {
            upperTerm = term;
            includeUpper = true;
            includeLower = true;
        } else if (op == TermOperator.LT) {
            upperTerm = term;
            includeLower = true;
        } else if (op == TermOperator.NE) {
            throw new QueryBuilderException("Term operator 'NE' not yet supported.");
        } else {
            throw new QueryBuilderException("Unknown term operator"); 
        }
        
        return new ConstantScoreRangeQuery(DocumentMapper.NAME_FIELD_NAME, 
                                           lowerTerm, 
                                           upperTerm, 
                                           includeLower, 
                                           includeUpper);
    }

}
