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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.FieldValueMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;
import org.vortikal.repositoryimpl.query.query.PropertyTermQuery;
import org.vortikal.repositoryimpl.query.query.TermOperator;

/**
 * 
 * @author oyviste
 *
 */
public class PropertyTermQueryBuilder implements QueryBuilder {

    private PropertyTermQuery ptq;
    public PropertyTermQueryBuilder(PropertyTermQuery ptq) {
        this.ptq = ptq;
    }

    public Query buildQuery() throws QueryBuilderException {
        
        // TODO: Use ConstantScoreRangeQuery to support
        //       other operators
        if (ptq.getOperator() != TermOperator.EQ) {
            throw new QueryBuilderException("Only the 'EQ' TermOperator is currently implemented");
        }
        
        PropertyTypeDefinition propDef = ptq.getPropertyDefinition();
        
        String fieldName = DocumentMapper.getFieldName(propDef);

        String fieldValue = FieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), 
                                                            propDef.getType());
        
        TermQuery tq = new TermQuery(new Term(fieldName, fieldValue));
        
        return tq;
        
    }

}
