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
package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyValidator;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

/**
 * XXX: Document me and evaluate usage?
 * XXX: Implement validation?
 */
public class PrincipalEvaluator implements CreatePropertyEvaluator, PropertyValidator {

    private PrincipalManager principalManager;
    
    protected Log logger = LogFactory.getLog(this.getClass());
    
    public boolean create(Principal principal, Property property, 
            PropertySet ancestorPropertySet, boolean isCollection, Date time) 
    throws PropertyEvaluationException {
        property.setPrincipalValue(principal);
        return true;
    }

    public void validate(Principal principal, PropertySet ancestorPropertySet, 
            Property property) throws ConstraintViolationException {
        
        if (property.getPrincipalValue() == null) {
            throw new ConstraintViolationException("Property must have a principal value");
        }

        Principal principalFromProperty = property.getPrincipalValue();
        if (!this.principalManager.validatePrincipal(principalFromProperty)) {
            throw new ConstraintViolationException(
                    "Unable to set " + property.getDefinition().getName() + " for resource to invalid principal: '" 
                    + principalFromProperty + "'");
        }
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
