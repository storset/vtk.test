/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationChallenge;


/**
 *  Default implementation of the Service interface.
 */
public class ServiceImpl implements Service, BeanNameAware, InitializingBean, Ordered {

    
    private AuthenticationChallenge authenticationChallenge;
    private Object handler;
    private List assertions = new ArrayList();
    private List services = new ArrayList();
    private Service parent;
    private String name;
    private List handlerInterceptors;
    private int order = Integer.MAX_VALUE; // Same as non ordered;
    private String category = null;

    // Duplicate:
    
    private LinkConstructor linkConstructor = new DefaultLinkConstructor();
	
    
    public void setHandler(Object handler) {
        this.handler = handler;
    }
	
    public void setAssertions(List assertions) {
        this.assertions = assertions;
    }
	

    public void setServices(List services) {
        this.services = services;
    }


    public List getChildren() {
        return services;
    }

    public Object getHandler() {
        return handler;
    }
	
    /**
     * Mapping the tree
     **/
    protected void setParent(Service parent) throws BeanInitializationException {
        if (this.parent != null) 
            throw new BeanInitializationException(
                "Service " + getName() +  "has at least two parents ("
                + parent.getName() + " and" + this.parent.getName() + ")");
        this.parent = parent;
    }
	
    public List getAssertions() {
        return assertions;
    }
	
    private List getAllAssertions() {
        if (parent == null) 
            return getAssertions();
 
        List assertions = new ArrayList(((ServiceImpl) parent).getAllAssertions());
        assertions.addAll(getAssertions());
        return assertions;
    }
	
    public String getName() {
        return name;
    }
	
    public void setBeanName(String name) {
        this.name = name;
    }


    public Service getParent() {
        return parent;
    }
	

    public String constructLink(Resource resource, Principal principal) {
        return constructLink(resource, principal, null, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                boolean matchAssertions) {
        return constructLink(resource, principal, null, matchAssertions);
    }

	
    public String constructLink(Resource resource, Principal principal,
                                Map parameters) {
        List assertions = getAllAssertions();
        return linkConstructor.construct(resource, principal, parameters, assertions,
                                         this, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                Map parameters, boolean matchAssertions) {
        List assertions = getAllAssertions();
        return linkConstructor.construct(resource, principal, parameters, assertions, this,
                                         matchAssertions);
    }
	
    
    public void afterPropertiesSet() throws Exception {

        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (! (o instanceof ServiceImpl)) {
                throw new BeanInitializationException(
                    "Only 'ServiceImpl' implementations of Service " +
                    "is supported ( check " + getName() +
                    "'s child services )");
            }
            ServiceImpl child = (ServiceImpl) o;
			
            validateAssertions(child);
			
            child.setParent(this);
        }
        
    }

    
    private void validateAssertions(ServiceImpl child) {
        List childAssertions = child.getAllAssertions();
		
        for (Iterator iter = getAssertions().iterator(); iter.hasNext();) {
            Assertion assertion = (Assertion) iter.next();
			
            for (Iterator iterator = childAssertions.iterator(); iterator
                     .hasNext();) {
                Assertion childAssertion = (Assertion) iterator.next();
				
                if (childAssertion.conflicts(assertion)) {
                    throw new BeanInitializationException(
                        "Assertion " +  assertion + " for service " +
                        getName() + " is conflicting with assertion " +
                        childAssertion + " in descendant node of child " 
                        + child.getName());
                }
            }
        }
    }

    
    public void setHandlerInterceptors(List handlerInterceptors) {
        this.handlerInterceptors = handlerInterceptors;
    }
    

    public List getHandlerInterceptors() {
        return handlerInterceptors;
    }

    
    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(getClass().getName()).append(": ").append(this.name);;
        return sb.toString();
    }

    public AuthenticationChallenge getAuthenticationChallenge() {
        return authenticationChallenge;
    }

    public void setAuthenticationChallenge(
        AuthenticationChallenge authenticationChallenge) {
        this.authenticationChallenge = authenticationChallenge;
    }
    
    /**
     * @return Returns the order.
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * @param order The order to set.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
