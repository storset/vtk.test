/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.text.tl.expr;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Stack;

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;

public abstract class NumericOperator extends Operator {

    private NumberFormat format = NumberFormat.getNumberInstance();
    
    public NumericOperator(Symbol symbol, Notation notation, Precedence precedence) {
        super(symbol, notation, precedence);
    }

    public final Object eval(Context ctx, Stack<Object> stack) {
        Object second = stack.pop();
        Object first = stack.pop();
        // Wrap values in BigDecimal to simplify calculations:
        BigDecimal n1 = new BigDecimal(getNumericValue(first).doubleValue());
        BigDecimal n2 = new BigDecimal(getNumericValue(second).doubleValue());
        Object result = evalNumeric(n1, n2);
        if (result instanceof BigDecimal) {
            BigDecimal d = (BigDecimal) result;
            if (d.scale() == 0) {
                result = d.intValueExact();
            } else {
                result = d.floatValue();
            }
        }
        return result;
    }

    protected abstract Object evalNumeric(BigDecimal n1, BigDecimal n2);

    private Number getNumericValue(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }

        if (obj instanceof Number) {
            return (Number) obj;
        } else if (obj instanceof String) {
            try {
                String s = (String) obj;
                return format.parse(s);
            } catch (ParseException e) { 
            }
        }
        throw new IllegalArgumentException("Not a number: " + obj);
    }
}
